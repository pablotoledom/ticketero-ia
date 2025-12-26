package com.example.config;

/**
 * Configuracion por ambiente (dev/prod).
 * Permite ajustar recursos segun necesidades y costos.
 */
public record EnvironmentConfig(
    String environmentName,
    String vpcCidr,
    int maxAzs,
    int natGateways,
    String dbInstanceClass,
    boolean dbMultiAz,
    String mqInstanceType,
    int ecsDesiredCount,
    int ecsMinCapacity,
    int ecsMaxCapacity,
    int ecsCpu,
    int ecsMemory
) {

    /**
     * Configuracion para ambiente de DESARROLLO.
     * Optimizado para bajo costo.
     */
    public static EnvironmentConfig dev() {
        return new EnvironmentConfig(
            "dev",              // environmentName
            "10.0.0.0/16",      // vpcCidr
            2,                  // maxAzs
            1,                  // natGateways (1 para ahorrar)
            "db.t3.micro",      // dbInstanceClass
            false,              // dbMultiAz (no en dev)
            "mq.t3.micro",      // mqInstanceType
            1,                  // ecsDesiredCount
            1,                  // ecsMinCapacity
            2,                  // ecsMaxCapacity
            512,                // ecsCpu
            1024                // ecsMemory
        );
    }

    /**
     * Configuracion para ambiente de PRODUCCION.
     * Optimizado para alta disponibilidad.
     */
    public static EnvironmentConfig prod() {
        return new EnvironmentConfig(
            "prod",             // environmentName
            "10.1.0.0/16",      // vpcCidr
            2,                  // maxAzs
            2,                  // natGateways (HA)
            "db.t3.small",      // dbInstanceClass
            true,               // dbMultiAz (si en prod)
            "mq.t3.micro",      // mqInstanceType
            2,                  // ecsDesiredCount
            2,                  // ecsMinCapacity
            6,                  // ecsMaxCapacity
            1024,               // ecsCpu
            2048                // ecsMemory
        );
    }

    /**
     * Prefijo para nombrar recursos.
     * Ejemplo: "ticketero-dev-vpc"
     */
    public String resourcePrefix() {
        return "ticketero-" + environmentName;
    }
}
