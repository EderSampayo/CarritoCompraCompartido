<?php

require 'vendor/autoload.php';

use Kreait\Firebase\Factory;
use Kreait\Firebase\Messaging\CloudMessage;
use Kreait\Firebase\Messaging\Notification;

// Configuración de Firebase Admin SDK
$factory = (new Factory)->withServiceAccount('../serviceAccount.json');

$messaging = $factory->createMessaging();

// Recibir datos POST
$data = json_decode(file_get_contents('php://input'), true);

// Check if tokens is set and is an array
if (isset($data['tokens']) && is_array($data['tokens'])) {
    $tokens = $data['tokens'];
} else {
    // Log error or handle the issue
    error_log("Error: 'tokens' is not set or not an array.");
    echo "Error: 'tokens' is not set or not an array.\n";
    exit();
}

$title = $data['title'] ?? 'No Title'; // Provide default values if not set
$body = $data['body'] ?? 'No Body';

$notification = Notification::create($title, $body);

// Enviar la notificación a todos los usuarios
foreach ($tokens as $token) {
    $message = CloudMessage::withTarget('token', $token)
        ->withNotification($notification);

    try {
        $messaging->send($message);
        echo "Notificación enviada a: $token\n";
    } catch (\Kreait\Firebase\Exception\MessagingException $e) {
        echo "Error al enviar la notificación a $token: " . $e->getMessage() . "\n";
    }
}
?>