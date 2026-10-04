<?php
require_once 'connects.php';

if (isset($_GET['email']) && isset($_GET['password'])) {
    $email = $_GET['email'];
    $password = $_GET['password'];

   
    $query = "SELECT password FROM users WHERE email = ?";
    $stmt = $con->prepare($query);
    $stmt->bind_param("s", $email);

    $stmt->execute();
    $stmt->store_result();

    if ($stmt->num_rows === 1) {
        $stmt->bind_result($storedPassword);
        $stmt->fetch();

        if ($storedPassword === $password) {
            echo "Login success";
        } else {
            echo "Incorrect password";
        }
    } else {
        echo "Email not found";
    }

    $stmt->close();
    $conn->close();
} else {
    echo "Missing parameters";
}
?>