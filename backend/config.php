<?php
// Copy this file's values to match your server. Keep real passwords out of version control:
// set them as environment variables (MR_DB_HOST, MR_DB_PORT, MR_DB_NAME, MR_DB_USER, MR_DB_PASS) instead.

return [
    'db_host' => getenv('MR_DB_HOST') ?: '127.0.0.1',
    'db_port' => getenv('MR_DB_PORT') ?: '3306',
    'db_name' => getenv('MR_DB_NAME') ?: 'malayan_reserves',
    'db_user' => getenv('MR_DB_USER') ?: 'root',
    'db_pass' => getenv('MR_DB_PASS') ?: '',
    'session_days' => 30,
];
