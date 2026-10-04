<?php
/**
 * Malayan Reserves JSON API.
 *
 * Every request is a POST to api.php?a=<action> with a JSON body. After login, send the session
 * token as "Authorization: Bearer <token>". Responses are {"ok": true, ...} or
 * {"ok": false, "error": "message", "code": "..."}. Times are epoch milliseconds.
 *
 * Actions: register, login, logout, me, vehicle, lot, mine, history, reserve, arrive, extend, end
 */

declare(strict_types=1);

const EMAIL_DOMAIN = '@live.mcl.edu.ph';
const ARRIVAL_WINDOW_MIN = 20;
const MIN_MINUTES = 60;
const MAX_MINUTES = 240;
const STEP_MIN = 30;
const EXTEND_MIN = 60;
const DAYS_AHEAD = 7;
const OPEN_HOUR = 6;
const CLOSE_HOUR = 22;
const MINUTE = 60000;
const VEHICLE_TYPES = ['Sedan', 'SUV', 'Hatchback', 'Coupe', 'Pickup', 'Van'];
const VEHICLE_COLORS = ['White', 'Black', 'Silver', 'Gray', 'Red', 'Blue', 'Green', 'Yellow', 'Orange', 'Brown'];
const LIVE = "('UPCOMING','HELD','ACTIVE')";

date_default_timezone_set('Asia/Manila');
header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store');

final class ApiError extends Exception
{
    public string $errorCode;

    public function __construct(string $message, int $status = 400, string $code = 'invalid')
    {
        parent::__construct($message, $status);
        $this->errorCode = $code;
    }
}

function respond(array $data, int $status = 200): void
{
    http_response_code($status);
    echo json_encode(['ok' => $status < 400] + $data, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit;
}

function fail(string $message, int $status = 400, string $code = 'invalid'): void
{
    throw new ApiError($message, $status, $code);
}

function now_ms(): int
{
    return (int) floor(microtime(true) * 1000);
}

function db(): PDO
{
    static $pdo = null;
    if ($pdo === null) {
        $c = require __DIR__ . '/config.php';
        $pdo = new PDO(
            "mysql:host={$c['db_host']};port={$c['db_port']};dbname={$c['db_name']};charset=utf8mb4",
            $c['db_user'],
            $c['db_pass'],
            [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION, PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC, PDO::ATTR_EMULATE_PREPARES => false]
        );
    }
    return $pdo;
}

function q(string $sql, array $args = []): PDOStatement
{
    $st = db()->prepare($sql);
    $st->execute($args);
    return $st;
}

// ---------- validation ----------

function normalize_plate(string $plate): string
{
    $p = strtoupper(trim($plate));
    $p = trim(preg_replace('/[^A-Z0-9]+/', ' ', $p));
    return preg_replace('/^([A-Z]{2,3})(\d)/', '$1 $2', $p);
}

function valid_plate(string $plate): bool
{
    return (bool) preg_match('/^[A-Z]{2,3} \d{3,5}$/', normalize_plate($plate));
}

function valid_email(string $email): bool
{
    $e = strtolower(trim($email));
    if (!str_ends_with($e, EMAIL_DOMAIN)) return false;
    return (bool) preg_match('/^[a-z0-9._%+-]+$/', substr($e, 0, -strlen(EMAIL_DOMAIN)));
}

function vehicle_from(array $body): array
{
    $v = $body['vehicle'] ?? null;
    if (!is_array($v)) fail('Add your vehicle details.');
    $type = (string) ($v['type'] ?? '');
    $color = (string) ($v['color'] ?? '');
    $plate = normalize_plate((string) ($v['plate'] ?? ''));
    if (!in_array($type, VEHICLE_TYPES, true)) fail('Choose your vehicle type.');
    if (!in_array($color, VEHICLE_COLORS, true)) fail('Choose your vehicle color.');
    if (!valid_plate($plate)) fail('Enter a plate number like ABC 1234.');
    return [$type, $color, $plate];
}

// ---------- sessions ----------

function token_from_header(): ?string
{
    $h = $_SERVER['HTTP_AUTHORIZATION'] ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION'] ?? '';
    if ($h === '' && function_exists('apache_request_headers')) {
        $headers = array_change_key_case(apache_request_headers(), CASE_LOWER);
        $h = $headers['authorization'] ?? '';
    }
    return preg_match('/^Bearer\s+([a-f0-9]{64})$/i', $h, $m) ? strtolower($m[1]) : null;
}

function current_user(): array
{
    $token = token_from_header();
    if ($token === null) fail('Please sign in again.', 401, 'auth');
    $u = q('SELECT u.* FROM sessions s JOIN users u ON u.id = s.user_id WHERE s.token_hash = ? AND s.expires_at > NOW()',
        [hash('sha256', $token)])->fetch();
    if (!$u) fail('Please sign in again.', 401, 'auth');
    return $u;
}

function start_session(int $userId): string
{
    $c = require __DIR__ . '/config.php';
    $token = bin2hex(random_bytes(32));
    q('INSERT INTO sessions (token_hash, user_id, expires_at) VALUES (?, ?, DATE_ADD(NOW(), INTERVAL ? DAY))',
        [hash('sha256', $token), $userId, (int) $c['session_days']]);
    q('DELETE FROM sessions WHERE expires_at < NOW()');
    return $token;
}

// ---------- output shapes ----------

function user_json(array $u): array
{
    return [
        'id' => (int) $u['id'], 'name' => $u['name'], 'email' => $u['email'], 'type' => $u['type'],
        'vehicle' => ['type' => $u['v_type'], 'color' => $u['v_color'], 'plate' => $u['plate']],
    ];
}

function reservation_json(array $r): array
{
    return [
        'id' => (int) $r['id'], 'slot' => $r['slot_id'], 'user_id' => (int) $r['user_id'],
        'start' => (int) $r['start_ms'], 'end' => (int) $r['end_ms'], 'status' => $r['status'],
        'arrived_at' => $r['arrived_at'] === null ? null : (int) $r['arrived_at'],
        'ended_at' => $r['ended_at'] === null ? null : (int) $r['ended_at'],
        'extended' => (bool) $r['extended'],
    ];
}

function mask_plate(string $plate): string
{
    $parts = explode(' ', normalize_plate($plate));
    if (count($parts) !== 2) return '•••';
    return $parts[0] . ' ' . str_repeat('•', max(1, strlen($parts[1]) - 2)) . substr($parts[1], -2);
}

// ---------- rules over time ----------

/** Moves reservations along as time passes: start → held → no-show, parked → completed. */
function refresh(): void
{
    $now = now_ms();
    q("UPDATE reservations SET status = 'HELD' WHERE status = 'UPCOMING' AND start_ms <= ?", [$now]);
    q("UPDATE reservations SET status = 'NO_SHOW', ended_at = start_ms + ? WHERE status = 'HELD' AND start_ms + ? <= ?",
        [ARRIVAL_WINDOW_MIN * MINUTE, ARRIVAL_WINDOW_MIN * MINUTE, $now]);
    q("UPDATE reservations SET status = 'COMPLETED', ended_at = end_ms WHERE status = 'ACTIVE' AND end_ms <= ?", [$now]);
}

function my_live(int $userId, bool $lock = false): ?array
{
    $r = q('SELECT * FROM reservations WHERE user_id = ? AND status IN ' . LIVE . ' ORDER BY start_ms LIMIT 1' . ($lock ? ' FOR UPDATE' : ''),
        [$userId])->fetch();
    return $r ?: null;
}

function has_conflict(string $slot, int $start, int $end, ?int $exceptId = null): bool
{
    $sql = 'SELECT COUNT(*) FROM reservations WHERE slot_id = ? AND status IN ' . LIVE . ' AND start_ms < ? AND end_ms > ?';
    $args = [$slot, $end, $start];
    if ($exceptId !== null) {
        $sql .= ' AND id <> ?';
        $args[] = $exceptId;
    }
    return (int) q($sql, $args)->fetchColumn() > 0;
}

function within_hours(int $start, int $end): bool
{
    $day = strtotime(date('Y-m-d', intdiv($start, 1000))) * 1000;
    return $start >= $day + OPEN_HOUR * 60 * MINUTE && $end <= $day + CLOSE_HOUR * 60 * MINUTE;
}

/** Runs [fn] in a transaction, rolling back on any error. */
function tx(callable $fn)
{
    db()->beginTransaction();
    try {
        $result = $fn();
        db()->commit();
        return $result;
    } catch (Throwable $e) {
        db()->rollBack();
        throw $e;
    }
}

// ---------- actions ----------

function act_register(array $b): array
{
    $name = trim((string) ($b['name'] ?? ''));
    $email = strtolower(trim((string) ($b['email'] ?? '')));
    $password = (string) ($b['password'] ?? '');
    $type = strtoupper((string) ($b['type'] ?? 'STUDENT'));
    if (mb_strlen($name) < 2 || mb_strlen($name) > 80) fail('Enter your full name.');
    if (!valid_email($email)) fail('Use your school email ending in ' . EMAIL_DOMAIN . '.');
    if (strlen($password) < 8) fail('Use a password with at least 8 characters.');
    if (!in_array($type, ['STUDENT', 'FACULTY', 'EMPLOYEE'], true)) fail('Choose student, faculty or employee.');
    [$vType, $vColor, $plate] = vehicle_from($b);

    if (q('SELECT 1 FROM users WHERE email = ?', [$email])->fetch()) fail('An account with this email already exists.', 409);
    if (q('SELECT 1 FROM users WHERE plate = ?', [$plate])->fetch()) fail('This plate number is already registered.', 409);
    q('INSERT INTO users (name, email, type, v_type, v_color, plate, pw_hash) VALUES (?, ?, ?, ?, ?, ?, ?)',
        [$name, $email, $type, $vType, $vColor, $plate, password_hash($password, PASSWORD_DEFAULT)]);
    $id = (int) db()->lastInsertId();
    $user = q('SELECT * FROM users WHERE id = ?', [$id])->fetch();
    return ['token' => start_session($id), 'user' => user_json($user)];
}

function act_login(array $b): array
{
    $email = strtolower(trim((string) ($b['email'] ?? '')));
    $password = (string) ($b['password'] ?? '');
    $u = q('SELECT * FROM users WHERE email = ?', [$email])->fetch();
    if (!$u || !password_verify($password, $u['pw_hash'])) fail('Incorrect email or password.', 401);
    if (password_needs_rehash($u['pw_hash'], PASSWORD_DEFAULT)) {
        q('UPDATE users SET pw_hash = ? WHERE id = ?', [password_hash($password, PASSWORD_DEFAULT), $u['id']]);
    }
    return ['token' => start_session((int) $u['id']), 'user' => user_json($u)];
}

function act_logout(): array
{
    $token = token_from_header();
    if ($token !== null) q('DELETE FROM sessions WHERE token_hash = ?', [hash('sha256', $token)]);
    return [];
}

function act_me(): array
{
    return ['user' => user_json(current_user())];
}

function act_vehicle(array $b): array
{
    $u = current_user();
    [$vType, $vColor, $plate] = vehicle_from($b);
    if (q('SELECT 1 FROM users WHERE plate = ? AND id <> ?', [$plate, $u['id']])->fetch()) fail('This plate number is already registered.', 409);
    q('UPDATE users SET v_type = ?, v_color = ?, plate = ? WHERE id = ?', [$vType, $vColor, $plate, $u['id']]);
    return ['user' => user_json(q('SELECT * FROM users WHERE id = ?', [$u['id']])->fetch())];
}

function act_lot(): array
{
    $me = (int) current_user()['id'];
    refresh();
    $now = now_ms();
    $slots = q('SELECT * FROM slots ORDER BY id')->fetchAll();
    $live = q('SELECT r.*, u.v_type, u.v_color, u.plate FROM reservations r JOIN users u ON u.id = r.user_id
               WHERE r.status IN ' . LIVE . ' ORDER BY r.start_ms')->fetchAll();
    $out = [];
    foreach ($slots as $s) {
        $mine = array_values(array_filter($live, fn($r) => $r['slot_id'] === $s['id']));
        $current = null;
        foreach ($mine as $r) {
            if ((int) $r['start_ms'] <= $now && in_array($r['status'], ['HELD', 'ACTIVE'], true)) { $current = $r; break; }
        }
        $state = 'AVAILABLE';
        $vehicle = null;
        if ($current !== null) {
            $isMine = (int) $current['user_id'] === $me;
            $state = $isMine ? 'MINE' : ($current['status'] === 'ACTIVE' ? 'OCCUPIED' : 'HELD');
            if ($isMine || $current['status'] === 'ACTIVE') {
                $vehicle = ['type' => $current['v_type'], 'color' => $current['v_color'],
                    'plate' => $isMine ? $current['plate'] : mask_plate($current['plate'])];
            }
        }
        $next = null;
        foreach ($mine as $r) {
            if ((int) $r['start_ms'] > $now) { $next = (int) $r['start_ms']; break; }
        }
        $out[] = [
            'id' => $s['id'], 'state' => $state, 'vehicle' => $vehicle,
            'until' => $current ? (int) $current['end_ms'] : null, 'next_start' => $next,
            'bookings' => array_map(fn($r) => ['start' => (int) $r['start_ms'], 'end' => (int) $r['end_ms'],
                'mine' => (int) $r['user_id'] === $me], $mine),
        ];
    }
    return ['now' => $now, 'slots' => $out];
}

function act_mine(): array
{
    $u = current_user();
    refresh();
    $r = my_live((int) $u['id']);
    return ['reservation' => $r ? reservation_json($r) : null];
}

function act_history(): array
{
    $u = current_user();
    refresh();
    $rows = q("SELECT * FROM reservations WHERE user_id = ? AND status IN ('COMPLETED','CANCELLED','NO_SHOW')
               ORDER BY start_ms DESC LIMIT 20", [$u['id']])->fetchAll();
    return ['reservations' => array_map('reservation_json', $rows)];
}

function act_reserve(array $b): array
{
    $u = current_user();
    refresh();
    $slotId = strtoupper((string) ($b['slot'] ?? ''));
    $start = (int) ($b['start'] ?? 0);
    $duration = (int) ($b['duration'] ?? 0);
    $now = now_ms();

    return tx(function () use ($u, $slotId, $start, $duration, $now) {
        // Lock the slot row and the user's live reservations so two requests can't double-book.
        $slot = q('SELECT * FROM slots WHERE id = ? FOR UPDATE', [$slotId])->fetch();
        if (!$slot) fail("That parking slot doesn't exist.", 404);
        if ((int) $slot['staff_only'] === 1 && $u['type'] === 'STUDENT') fail("{$slot['id']} is reserved for faculty and staff.", 403);
        if ($duration < MIN_MINUTES || $duration > MAX_MINUTES || $duration % STEP_MIN !== 0) fail('Choose a duration between 1 and 4 hours.');

        $parkNow = $start <= $now + MINUTE;
        $begin = $parkNow ? $now : $start;
        $end = $begin + $duration * MINUTE;
        if (!$parkNow) {
            if ($begin % (STEP_MIN * MINUTE) !== 0) fail('Start times are on the hour or half hour.');
            if ($begin > $now + DAYS_AHEAD * 24 * 60 * MINUTE) fail('You can book up to ' . DAYS_AHEAD . ' days ahead.');
            if (!within_hours($begin, $end)) fail('Advance bookings run from 6 AM to 10 PM.');
        }
        if (my_live((int) $u['id'], true)) fail('You already have a reservation. Finish or cancel it first.', 409);
        if (has_conflict($slot['id'], $begin, $end)) fail("{$slot['id']} is already booked for part of that time.", 409);

        q('INSERT INTO reservations (slot_id, user_id, start_ms, end_ms, status) VALUES (?, ?, ?, ?, ?)',
            [$slot['id'], $u['id'], $begin, $end, $parkNow ? 'HELD' : 'UPCOMING']);
        $r = q('SELECT * FROM reservations WHERE id = ?', [db()->lastInsertId()])->fetch();
        return ['reservation' => reservation_json($r)];
    });
}

function act_arrive(): array
{
    $u = current_user();
    refresh();
    return tx(function () use ($u) {
        $now = now_ms();
        $r = my_live((int) $u['id'], true);
        if (!$r) fail("You don't have a reservation.", 404);
        if ($r['status'] === 'ACTIVE') fail("You're already checked in.", 409);
        $start = (int) $r['start_ms'];
        if ($r['status'] === 'UPCOMING') {
            if ($start - $now > 15 * MINUTE) fail('You can check in up to 15 minutes before your start time.');
            if (has_conflict($r['slot_id'], $now, $start, (int) $r['id'])) fail("{$r['slot_id']} is still in use. Please wait until your start time.", 409);
        }
        q("UPDATE reservations SET status = 'ACTIVE', arrived_at = ?, start_ms = LEAST(start_ms, ?) WHERE id = ?", [$now, $now, $r['id']]);
        return ['reservation' => reservation_json(q('SELECT * FROM reservations WHERE id = ?', [$r['id']])->fetch())];
    });
}

function act_extend(): array
{
    $u = current_user();
    refresh();
    return tx(function () use ($u) {
        $r = my_live((int) $u['id'], true);
        if (!$r) fail("You don't have a reservation.", 404);
        if ((int) $r['extended'] === 1) fail('A reservation can only be extended once.', 409);
        q('SELECT id FROM slots WHERE id = ? FOR UPDATE', [$r['slot_id']]);
        $newEnd = (int) $r['end_ms'] + EXTEND_MIN * MINUTE;
        if (has_conflict($r['slot_id'], (int) $r['end_ms'], $newEnd, (int) $r['id'])) {
            fail("Someone has booked {$r['slot_id']} right after you, so it can't be extended.", 409);
        }
        q('UPDATE reservations SET end_ms = ?, extended = 1 WHERE id = ?', [$newEnd, $r['id']]);
        return ['reservation' => reservation_json(q('SELECT * FROM reservations WHERE id = ?', [$r['id']])->fetch())];
    });
}

function act_end(): array
{
    $u = current_user();
    refresh();
    return tx(function () use ($u) {
        $r = my_live((int) $u['id'], true);
        if (!$r) fail("You don't have a reservation.", 404);
        $status = $r['status'] === 'ACTIVE' ? 'COMPLETED' : 'CANCELLED';
        q('UPDATE reservations SET status = ?, ended_at = ? WHERE id = ?', [$status, now_ms(), $r['id']]);
        return ['reservation' => reservation_json(q('SELECT * FROM reservations WHERE id = ?', [$r['id']])->fetch())];
    });
}

// ---------- router ----------

try {
    if ($_SERVER['REQUEST_METHOD'] !== 'POST') fail('Use POST with a JSON body.', 405);
    $raw = file_get_contents('php://input') ?: '{}';
    $body = json_decode($raw, true);
    if (!is_array($body)) fail('The request body must be JSON.');

    $action = (string) ($_GET['a'] ?? '');
    $routes = [
        'register' => fn() => act_register($body),
        'login' => fn() => act_login($body),
        'logout' => fn() => act_logout(),
        'me' => fn() => act_me(),
        'vehicle' => fn() => act_vehicle($body),
        'lot' => fn() => act_lot(),
        'mine' => fn() => act_mine(),
        'history' => fn() => act_history(),
        'reserve' => fn() => act_reserve($body),
        'arrive' => fn() => act_arrive(),
        'extend' => fn() => act_extend(),
        'end' => fn() => act_end(),
    ];
    if (!isset($routes[$action])) fail('Unknown action.', 404);
    respond($routes[$action]());
} catch (ApiError $e) {
    respond(['error' => $e->getMessage(), 'code' => $e->errorCode], $e->getCode());
} catch (Throwable $e) {
    error_log('Malayan Reserves API: ' . $e->getMessage());
    respond(['error' => 'The server had a problem. Please try again.', 'code' => 'server'], 500);
}
