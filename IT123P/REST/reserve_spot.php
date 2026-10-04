<?php
include 'connects.php';

$email = $_POST['email'];
$slotname = $_POST['slotname'];
$resdate = $_POST['resdate'];  // format: YYYY-MM-DD
$time = $_POST['time'];        // format: HH:MM:SS
$mode = $_POST['mode'];        // "now" or "later"

// get userid
$stmt = $con->prepare("SELECT userid FROM users WHERE email = ?");
$stmt->bind_param("s", $email);
$stmt->execute();
$stmt->bind_result($userid);
if (!$stmt->fetch()) {
    echo "error: user not found";
    $stmt->close();
    $con->close();
    exit();
}
$stmt->close();

// get slotid
$stmt = $con->prepare("SELECT slotid FROM parkslots WHERE slotname = ?");
$stmt->bind_param("s", $slotname);
$stmt->execute();
$stmt->bind_result($slotid);
if (!$stmt->fetch()) {
    echo "error: slot not found";
    $stmt->close();
    $con->close();
    exit();
}
$stmt->close();

// generate new resid
$res = $con->query("SELECT MAX(resid) AS mx FROM reservations");
$row = $res->fetch_assoc();
$next = str_pad(((int)$row['mx']) + 1, 5, '0', STR_PAD_LEFT);

// insert reservation
$stmt = $con->prepare("
    INSERT INTO reservations (resid, slotid, userid, resdate, time)
    VALUES (?, ?, ?, ?, ?)");
$stmt->bind_param("sssss", $next, $slotid, $userid, $resdate, $time);

if ($stmt->execute()) {
    // Only insert into usedparking if mode is 'now' and resdate is today
    $today = date("Y-m-d");
    if ($mode === "now" && $resdate === $today) {
        $usedStmt = $con->prepare("
            INSERT INTO usedparking (resid, slotid, userid)
            VALUES (?, ?, ?)");
        $usedStmt->bind_param("sss", $next, $slotid, $userid);
        $usedStmt->execute();
        $usedStmt->close();
    }

    echo "success";
} else {
    echo "error: " . $stmt->error;
}

$stmt->close();
$con->close();
?>
