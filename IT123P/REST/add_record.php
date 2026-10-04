<?php
include_once('connects.php');

// Avoid fatal error on SQL issues
mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

try {
    // Fetch GET parameters
    $email = $_GET['email'] ?? '';
    $password = $_GET['password'] ?? '';
    $user_type = $_GET['user_type'] ?? '';
    $car_type = $_GET['car_type'] ?? '';
    $car_color = $_GET['car_color'] ?? '';
    $plate = $_GET['plate'] ?? '';

    // Validate required fields
    if (!$email || !$password || !$user_type || !$car_type || !$car_color || !$plate) {
        http_response_code(400);
        echo "Missing required fields";
        exit();
    }

    // Generate next userid in format "001", "002", etc.
    $query = "SELECT MAX(CAST(userid AS UNSIGNED)) AS max_id FROM users";
    $result = mysqli_query($con, $query);
    $row = mysqli_fetch_assoc($result);
    $nextId = str_pad(((int)$row['max_id']) + 1, 3, '0', STR_PAD_LEFT);

    // Prepare and execute insert
    $stmt = $con->prepare("INSERT INTO users (userid, email, password, user_type, car_type, car_color, plate)
                           VALUES (?, ?, ?, ?, ?, ?, ?)");
    $stmt->bind_param("sssssss", $nextId, $email, $password, $user_type, $car_type, $car_color, $plate);
    $stmt->execute();

    echo "success";
} catch (mysqli_sql_exception $e) {
    http_response_code(500);
    echo "MySQL ERROR: " . $e->getMessage();
}
?>