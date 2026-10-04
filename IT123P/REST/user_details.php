<?php
require_once 'connects.php';

if (isset($_GET['email'])) {
    $email = $_GET['email'];

    $query = "SELECT user_type, car_type, car_color, plate FROM users WHERE email = ?";
    $stmt = $con->prepare($query);
    $stmt->bind_param("s", $email);
    $stmt->execute();
    $stmt->store_result();

    if ($stmt->num_rows === 1) {
        $stmt->bind_result($user_type, $car_type, $car_color, $plate);
        $stmt->fetch();

        echo json_encode([
            "usertype" => $user_type,
            "cartype" => $car_type,
            "carcolor" => $car_color,
            "plate" => $plate
        ]);
    } else {
        echo json_encode(["error" => "User not found"]);
    }

    $stmt->close();
    $con->close();
} else {
    echo json_encode(["error" => "Missing email parameter"]);
}
?>
