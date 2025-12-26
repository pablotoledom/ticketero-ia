package com.example.stacks;

import com.example.config.EnvironmentConfig;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.IpAddresses;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ec2.SubnetConfiguration;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.CfnOutput;
import software.constructs.Construct;

import java.util.List;

/**
 * Stack de red: VPC, Subnets, Security Groups.
 * Este stack NO tiene dependencias, se crea primero.
 */
public class NetworkStack extends Stack {

    private final Vpc vpc;
    private final SecurityGroup albSecurityGroup;
    private final SecurityGroup appSecurityGroup;
    private final SecurityGroup dbSecurityGroup;
    private final SecurityGroup mqSecurityGroup;

    public NetworkStack(final Construct scope, final String id,
                        final StackProps props, final EnvironmentConfig config) {
        super(scope, id, props);

        String prefix = config.resourcePrefix();

        // ========================================
        // VPC - Red principal
        // ========================================
        this.vpc = Vpc.Builder.create(this, "Vpc")
            .vpcName(prefix + "-vpc")
            .ipAddresses(IpAddresses.cidr(config.vpcCidr()))
            .maxAzs(config.maxAzs())
            .natGateways(config.natGateways())
            .subnetConfiguration(List.of(
                // Subnets publicas - para Load Balancer
                SubnetConfiguration.builder()
                    .name("public")
                    .subnetType(SubnetType.PUBLIC)
                    .cidrMask(24)
                    .build(),
                // Subnets privadas - para App, DB, MQ
                SubnetConfiguration.builder()
                    .name("private")
                    .subnetType(SubnetType.PRIVATE_WITH_EGRESS)
                    .cidrMask(24)
                    .build()
            ))
            .build();

        // ========================================
        // Security Group - Load Balancer (ALB)
        // ========================================
        this.albSecurityGroup = SecurityGroup.Builder.create(this, "AlbSg")
            .securityGroupName(prefix + "-alb-sg")
            .description("Security group para Application Load Balancer")
            .vpc(vpc)
            .allowAllOutbound(true)
            .build();

        // Permitir HTTP desde internet
        albSecurityGroup.addIngressRule(
            Peer.anyIpv4(),
            Port.tcp(80),
            "Permitir HTTP desde internet"
        );

        // Permitir HTTPS desde internet (para futuro)
        albSecurityGroup.addIngressRule(
            Peer.anyIpv4(),
            Port.tcp(443),
            "Permitir HTTPS desde internet"
        );

        // ========================================
        // Security Group - Aplicacion (ECS)
        // ========================================
        this.appSecurityGroup = SecurityGroup.Builder.create(this, "AppSg")
            .securityGroupName(prefix + "-app-sg")
            .description("Security group para contenedores ECS")
            .vpc(vpc)
            .allowAllOutbound(true)
            .build();

        // Permitir trafico desde ALB al puerto 8080
        appSecurityGroup.addIngressRule(
            albSecurityGroup,
            Port.tcp(8080),
            "Permitir trafico desde ALB"
        );

        // ========================================
        // Security Group - Base de Datos (RDS)
        // ========================================
        this.dbSecurityGroup = SecurityGroup.Builder.create(this, "DbSg")
            .securityGroupName(prefix + "-db-sg")
            .description("Security group para RDS PostgreSQL")
            .vpc(vpc)
            .allowAllOutbound(false)  // DB no necesita salida
            .build();

        // Permitir conexiones PostgreSQL solo desde la aplicacion
        dbSecurityGroup.addIngressRule(
            appSecurityGroup,
            Port.tcp(5432),
            "Permitir PostgreSQL desde App"
        );

        // ========================================
        // Security Group - Mensajeria (Amazon MQ)
        // ========================================
        this.mqSecurityGroup = SecurityGroup.Builder.create(this, "MqSg")
            .securityGroupName(prefix + "-mq-sg")
            .description("Security group para Amazon MQ RabbitMQ")
            .vpc(vpc)
            .allowAllOutbound(false)
            .build();

        // Permitir AMQP desde la aplicacion
        mqSecurityGroup.addIngressRule(
            appSecurityGroup,
            Port.tcp(5671),
            "Permitir AMQP desde App"
        );

        // Permitir consola de administracion desde la aplicacion
        mqSecurityGroup.addIngressRule(
            appSecurityGroup,
            Port.tcp(443),
            "Permitir MQ Console desde App"
        );

        // ========================================
        // Outputs - Valores para otros stacks
        // ========================================
        CfnOutput.Builder.create(this, "VpcId")
            .exportName(prefix + "-vpc-id")
            .value(vpc.getVpcId())
            .description("ID de la VPC")
            .build();

        CfnOutput.Builder.create(this, "VpcCidr")
            .exportName(prefix + "-vpc-cidr")
            .value(config.vpcCidr())
            .description("CIDR de la VPC")
            .build();
    }

    // ========================================
    // Getters - Para que otros stacks accedan
    // ========================================

    public Vpc getVpc() {
        return vpc;
    }

    public SecurityGroup getAlbSecurityGroup() {
        return albSecurityGroup;
    }

    public SecurityGroup getAppSecurityGroup() {
        return appSecurityGroup;
    }

    public SecurityGroup getDbSecurityGroup() {
        return dbSecurityGroup;
    }

    public SecurityGroup getMqSecurityGroup() {
        return mqSecurityGroup;
    }
}
