package com.example.stacks;

import com.example.config.EnvironmentConfig;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.services.secretsmanager.Secret;
import software.amazon.awscdk.services.secretsmanager.SecretStringGenerator;
import software.constructs.Construct;

/**
 * Stack de notificaciones: Secretos para Telegram y otros canales.
 * Este stack maneja las credenciales de servicios de notificacion.
 */
public class NotificationStack extends Stack {

    private final Secret telegramSecret;

    public NotificationStack(final Construct scope, final String id,
                             final StackProps props, final EnvironmentConfig config) {
        super(scope, id, props);

        String prefix = config.resourcePrefix();

        // ========================================
        // Secret - Credenciales de Telegram
        // ========================================
        // El secreto se crea con valores placeholder.
        // Actualizar manualmente en AWS Console con valores reales:
        //   - botToken: Token del bot de Telegram (de @BotFather)
        //   - chatId: ID del chat donde enviar notificaciones
        this.telegramSecret = Secret.Builder.create(this, "TelegramSecret")
            .secretName(prefix + "-telegram-credentials")
            .description("Credenciales para notificaciones de Telegram")
            .generateSecretString(SecretStringGenerator.builder()
                .secretStringTemplate("{\"botToken\": \"PLACEHOLDER_UPDATE_ME\", \"chatId\": \"PLACEHOLDER_UPDATE_ME\"}")
                .generateStringKey("apiKey")  // Campo extra por si se necesita
                .excludePunctuation(true)
                .passwordLength(16)
                .build())
            .build();

        // ========================================
        // Outputs
        // ========================================
        CfnOutput.Builder.create(this, "TelegramSecretArn")
            .exportName(prefix + "-telegram-secret-arn")
            .value(telegramSecret.getSecretArn())
            .description("ARN del secreto con credenciales de Telegram")
            .build();

        CfnOutput.Builder.create(this, "TelegramSecretName")
            .exportName(prefix + "-telegram-secret-name")
            .value(telegramSecret.getSecretName())
            .description("Nombre del secreto de Telegram")
            .build();
    }

    // ========================================
    // Getters
    // ========================================

    public Secret getTelegramSecret() {
        return telegramSecret;
    }
}
