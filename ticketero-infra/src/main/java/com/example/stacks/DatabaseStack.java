package com.example.stacks;

import com.example.config.EnvironmentConfig;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.rds.DatabaseInstance;
import software.amazon.awscdk.services.rds.DatabaseInstanceEngine;
import software.amazon.awscdk.services.rds.PostgresEngineVersion;
import software.amazon.awscdk.services.rds.PostgresInstanceEngineProps;
import software.amazon.awscdk.services.rds.Credentials;
import software.amazon.awscdk.services.rds.StorageType;
import software.amazon.awscdk.services.secretsmanager.Secret;
import software.amazon.awscdk.services.secretsmanager.SecretStringGenerator;
import software.constructs.Construct;

/**
 * Stack de base de datos: RDS PostgreSQL.
 * Depende de NetworkStack para VPC y Security Groups.
 */
public class DatabaseStack extends Stack {

    private final DatabaseInstance database;
    private final Secret databaseSecret;

    public DatabaseStack(final Construct scope, final String id,
                         final StackProps props, final EnvironmentConfig config,
                         final NetworkStack networkStack) {
        super(scope, id, props);

        String prefix = config.resourcePrefix();

        // ========================================
        // Secret - Credenciales de la base de datos
        // ========================================
        this.databaseSecret = Secret.Builder.create(this, "DbSecret")
            .secretName(prefix + "-db-credentials")
            .description("Credenciales para RDS PostgreSQL")
            .generateSecretString(SecretStringGenerator.builder()
                .secretStringTemplate("{\"username\": \"ticketero\"}")
                .generateStringKey("password")
                .excludePunctuation(true)  // Evitar caracteres problematicos
                .passwordLength(32)
                .build())
            .build();

        // ========================================
        // Determinar tamano de instancia
        // ========================================
        InstanceType instanceType = parseInstanceType(config.dbInstanceClass());

        // ========================================
        // RDS PostgreSQL
        // ========================================
        this.database = DatabaseInstance.Builder.create(this, "Database")
            .instanceIdentifier(prefix + "-postgres")
            .engine(DatabaseInstanceEngine.postgres(PostgresInstanceEngineProps.builder()
                .version(PostgresEngineVersion.VER_16_4)
                .build()))
            .instanceType(instanceType)
            .vpc(networkStack.getVpc())
            .vpcSubnets(SubnetSelection.builder()
                .subnetType(SubnetType.PRIVATE_WITH_EGRESS)
                .build())
            .securityGroups(java.util.List.of(networkStack.getDbSecurityGroup()))
            .credentials(Credentials.fromSecret(databaseSecret))
            .databaseName("ticketero")
            .port(5432)
            .storageType(StorageType.GP3)
            .allocatedStorage(20)  // 20 GB inicial
            .maxAllocatedStorage(100)  // Auto-escala hasta 100 GB
            .multiAz(config.dbMultiAz())
            .publiclyAccessible(false)  // Solo acceso interno
            .backupRetention(config.environmentName().equals("prod")
                ? Duration.days(7)
                : Duration.days(1))
            .deletionProtection(config.environmentName().equals("prod"))
            .removalPolicy(config.environmentName().equals("prod")
                ? RemovalPolicy.RETAIN
                : RemovalPolicy.DESTROY)
            .build();

        // ========================================
        // Outputs
        // ========================================
        CfnOutput.Builder.create(this, "DbEndpoint")
            .exportName(prefix + "-db-endpoint")
            .value(database.getDbInstanceEndpointAddress())
            .description("Endpoint de la base de datos")
            .build();

        CfnOutput.Builder.create(this, "DbPort")
            .exportName(prefix + "-db-port")
            .value(database.getDbInstanceEndpointPort())
            .description("Puerto de la base de datos")
            .build();

        CfnOutput.Builder.create(this, "DbSecretArn")
            .exportName(prefix + "-db-secret-arn")
            .value(databaseSecret.getSecretArn())
            .description("ARN del secreto con credenciales")
            .build();
    }

    /**
     * Convierte string como "db.t3.micro" a InstanceType.
     */
    private InstanceType parseInstanceType(String instanceClass) {
        // db.t3.micro -> T3, MICRO
        String[] parts = instanceClass.replace("db.", "").split("\\.");
        String classStr = parts[0].toUpperCase();  // t3 -> T3
        String sizeStr = parts[1].toUpperCase();   // micro -> MICRO

        InstanceClass ic = InstanceClass.valueOf(classStr);
        InstanceSize is = InstanceSize.valueOf(sizeStr);

        return InstanceType.of(ic, is);
    }

    // ========================================
    // Getters
    // ========================================

    public DatabaseInstance getDatabase() {
        return database;
    }

    public Secret getDatabaseSecret() {
        return databaseSecret;
    }

    public String getJdbcUrl() {
        return String.format("jdbc:postgresql://%s:%s/ticketero",
            database.getDbInstanceEndpointAddress(),
            database.getDbInstanceEndpointPort());
    }
}
