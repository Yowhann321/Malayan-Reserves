<?php
include 'connects.php';

// Fetch all slotids from usedparking
$query = "
    SELECT p.slotname 
    FROM usedparking u
    JOIN parkslots p ON u.slotid = p.slotid
";

$result = $con->query($query);

$usedSlots = [];

if ($result && $result->num_rows > 0) {
    while ($row = $result->fetch_assoc()) {
        $usedSlots[] = $row['slotname'];
    }
}

echo json_encode($usedSlots);

$con->close();
?>
