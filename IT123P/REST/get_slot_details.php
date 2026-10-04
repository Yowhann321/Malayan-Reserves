<?php
require 'connects.php';

if (isset($_GET['slotname'])) {
    $slotname = $_GET['slotname'];

    $query = "SELECT u.user_type, u.car_type, u.car_color, u.plate
              FROM usedparking up
              JOIN users u ON up.userid = u.email
              WHERE up.slotid = ?
              ORDER BY up.resid DESC
              LIMIT 1";

    $stmt = $con->prepare($query);
    $stmt->bind_param("s", $slotname);
    $stmt->execute();
    $result = $stmt->get_result();

    if ($result && $row = $result->fetch_assoc()) {
        echo json_encode([
            "usertype" => $row['user_type'],
            "cartype" => $row['car_type'],
            "carcolor" => $row['car_color'],
            "plate" => $row['plate']
        ]);
    } else {
        echo json_encode(["error" => "No reservation found"]);
    }

    $stmt->close();
    $con->close();
}
?>
