package com.example.stacks;

import com.example.config.EnvironmentConfig;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ecr.assets.DockerImageAsset;
import software.amazon.awscdk.services.ecr.assets.Platform;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.CpuArchitecture;
import software.amazon.awscdk.services.ecs.OperatingSystemFamily;
import software.amazon.awscdk.services.ecs.RuntimePlatform;
import software.amazon.awscdk.services.ecs.Secret;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.constructs.Construct;

import java.util.Map;

/**
 * Stack de aplicacion: ECS Fargate + ALB.
 * Depende de todos los demas stacks.
 */
public class ApplicationStack extends Stack {

    private final ApplicationLoadBalancedFargateService service;

    public ApplicationStack(final Construct scope, final String id,
                            final StackProps props, final EnvironmentConfig config,
                            final NetworkStack networkStack,
                            final DatabaseStack databaseStack,
                            final MessagingStack messagingStack,
                            final NotificationStack notificationStack) {
        super(scope, id, props);

        String prefix = config.resourcePrefix();

        // ========================================
        // CloudWatch Logs - Para ver logs de la app
        // ========================================
        LogGroup logGroup = LogGroup.Builder.create(this, "LogGroup")
            .logGroupName("/ecs/" + prefix)
            .retention(RetentionDays.TWO_WEEKS)
            .build();

        // ========================================
        // ECS Cluster
        // ========================================
        Cluster cluster = Cluster.Builder.create(this, "Cluster")
            .clusterName(prefix + "-cluster")
            .vpc(networkStack.getVpc())
            .containerInsights(true)
            .build();

        // ========================================
        // Docker Image - Construir desde codigo fuente
        // ========================================
        DockerImageAsset dockerImage = DockerImageAsset.Builder.create(this, "DockerImage")
            .directory("../ticketero")  // Ruta al Dockerfile
            .platform(Platform.LINUX_AMD64)
            .build();

        // ========================================
        // Variables de entorno para la aplicacion
        // ========================================
        Map<String, String> environment = Map.ofEntries(
            Map.entry("SPRING_PROFILES_ACTIVE", config.environmentName()),
            Map.entry("SERVER_PORT", "8080"),
            // Database
            Map.entry("DATABASE_URL", String.format("jdbc:postgresql://%s:%s/ticketero",
                databaseStack.getDatabase().getDbInstanceEndpointAddress(),
                databaseStack.getDatabase().getDbInstanceEndpointPort())),
            // RabbitMQ - endpoint correcto usa .on.aws
            Map.entry("RABBITMQ_HOST", messagingStack.getBroker().getRef() + ".mq." +
                this.getRegion() + ".on.aws"),
            Map.entry("RABBITMQ_PORT", "5671"),
            Map.entry("RABBITMQ_SSL", "true"),
            // Telegram - habilitado via secreto
            Map.entry("TELEGRAM_ENABLED", "false")
        );

        // ========================================
        // Secretos (credenciales seguras)
        // ========================================
        Map<String, Secret> secrets = Map.of(
            "DATABASE_USERNAME", Secret.fromSecretsManager(
                databaseStack.getDatabaseSecret(), "username"),
            "DATABASE_PASSWORD", Secret.fromSecretsManager(
                databaseStack.getDatabaseSecret(), "password"),
            "RABBITMQ_USERNAME", Secret.fromSecretsManager(
                messagingStack.getMqSecret(), "username"),
            "RABBITMQ_PASSWORD", Secret.fromSecretsManager(
                messagingStack.getMqSecret(), "password"),
            // Telegram credentials from Secrets Manager
            "TELEGRAM_BOT_TOKEN", Secret.fromSecretsManager(
                notificationStack.getTelegramSecret(), "botToken"),
            "TELEGRAM_CHAT_ID", Secret.fromSecretsManager(
                notificationStack.getTelegramSecret(), "chatId")
        );

        // ========================================
        // ECS Fargate Service + ALB (todo en uno)
        // ========================================
        this.service = ApplicationLoadBalancedFargateService.Builder.create(this, "Service")
            .serviceName(prefix + "-service")
            .cluster(cluster)
            .cpu(config.ecsCpu())
            .memoryLimitMiB(config.ecsMemory())
            .desiredCount(config.ecsDesiredCount())
            .runtimePlatform(RuntimePlatform.builder()
                .cpuArchitecture(CpuArchitecture.X86_64)
                .operatingSystemFamily(OperatingSystemFamily.LINUX)
                .build())
            .taskImageOptions(ApplicationLoadBalancedTaskImageOptions.builder()
                .image(ContainerImage.fromDockerImageAsset(dockerImage))
                .containerPort(8080)
                .environment(environment)
                .secrets(secrets)
                .logDriver(software.amazon.awscdk.services.ecs.LogDrivers.awsLogs(
                    software.amazon.awscdk.services.ecs.AwsLogDriverProps.builder()
                        .logGroup(logGroup)
                        .streamPrefix("ticketero")
                        .build()))
                .build())
            .publicLoadBalancer(true)
            .taskSubnets(SubnetSelection.builder()
                .subnetType(SubnetType.PRIVATE_WITH_EGRESS)
                .build())
            .loadBalancerName(prefix + "-alb")
            .healthCheckGracePeriod(Duration.seconds(180))
            .build();

        // Permitir trafico desde el ALB al servicio en puerto 8080
        service.getService().getConnections().allowFrom(
            service.getLoadBalancer(),
            software.amazon.awscdk.services.ec2.Port.tcp(8080),
            "Permitir trafico desde ALB"
        );

        // Permitir conexion a la base de datos
        service.getService().getConnections().allowTo(
            networkStack.getDbSecurityGroup(),
            software.amazon.awscdk.services.ec2.Port.tcp(5432),
            "Permitir conexion a PostgreSQL"
        );

        // Permitir conexion a RabbitMQ
        service.getService().getConnections().allowTo(
            networkStack.getMqSecurityGroup(),
            software.amazon.awscdk.services.ec2.Port.tcp(5671),
            "Permitir conexion a RabbitMQ"
        );

        // ========================================
        // Health Check - Verificar que la app responde
        // ========================================
        service.getTargetGroup().configureHealthCheck(HealthCheck.builder()
            .path("/actuator/health")
            .port("8080")
            .healthyHttpCodes("200")
            .interval(Duration.seconds(30))
            .timeout(Duration.seconds(10))
            .healthyThresholdCount(2)
            .unhealthyThresholdCount(3)
            .build());

        // ========================================
        // Auto Scaling
        // ========================================
        var scaling = service.getService().autoScaleTaskCount(
            software.amazon.awscdk.services.applicationautoscaling.EnableScalingProps.builder()
                .minCapacity(config.ecsMinCapacity())
                .maxCapacity(config.ecsMaxCapacity())
                .build());

        scaling.scaleOnCpuUtilization("CpuScaling",
            software.amazon.awscdk.services.ecs.CpuUtilizationScalingProps.builder()
                .targetUtilizationPercent(70)
                .scaleInCooldown(Duration.seconds(60))
                .scaleOutCooldown(Duration.seconds(60))
                .build());

        // ========================================
        // Outputs
        // ========================================
        CfnOutput.Builder.create(this, "LoadBalancerDns")
            .exportName(prefix + "-alb-dns")
            .value(service.getLoadBalancer().getLoadBalancerDnsName())
            .description("URL del Application Load Balancer")
            .build();

        CfnOutput.Builder.create(this, "ServiceUrl")
            .exportName(prefix + "-service-url")
            .value("http://" + service.getLoadBalancer().getLoadBalancerDnsName())
            .description("URL de la aplicacion")
            .build();

        CfnOutput.Builder.create(this, "ClusterName")
            .exportName(prefix + "-cluster-name")
            .value(cluster.getClusterName())
            .description("Nombre del cluster ECS")
            .build();
    }

    // ========================================
    // Getters
    // ========================================

    public ApplicationLoadBalancedFargateService getService() {
        return service;
    }
}
