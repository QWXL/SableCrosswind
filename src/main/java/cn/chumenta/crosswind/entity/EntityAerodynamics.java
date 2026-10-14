package cn.chumenta.crosswind.entity;

import org.joml.Vector3d;

public final class EntityAerodynamics {
    private EntityAerodynamics() {
    }

    public static Vector3d horizontalDrive(final double windX, final double windZ,
                                           final double entityX, final double entityZ,
                                           final Vector3d dest) {
        return dest.set(windX - entityX, 0.0D, windZ - entityZ);
    }

    public static double projectedArea(final double sizeX, final double sizeY, final double sizeZ,
                                       final double windX, final double windZ) {
        final double windLen = Math.sqrt(windX * windX + windZ * windZ);
        if (windLen < 1.0E-9D) {
            return 0.0D;
        }
        if (sizeX <= 0.0D || sizeY <= 0.0D || sizeZ <= 0.0D) {
            return 0.0D;
        }
        final double nx = Math.abs(windX) / windLen;
        final double nz = Math.abs(windZ) / windLen;

        return nx * sizeY * sizeZ + nz * sizeX * sizeY;
    }

    public static double effectiveMass(final double volume, final double kMass, final double vMin) {
        return kMass * Math.max(volume, vMin);
    }

    public static double resistanceFactor(final double volume, final double vRef, final double kResist) {
        if (kResist == 0.0D || vRef <= 0.0D || volume <= 0.0D) {
            return 1.0D;
        }

        return Math.pow(volume / vRef, kResist);
    }

    public static double effectiveMassWithResistance(final double volume, final double kMass,
                                                     final double vMin, final double vRef,
                                                     final double kResist) {
        return effectiveMass(volume, kMass, vMin) * resistanceFactor(volume, vRef, kResist);
    }

    public static Vector3d acceleration(final Vector3d drive, final double area,
                                        final double effMass, final double density,
                                        final double cD, final double maxAccel,
                                        final Vector3d out) {
        if (area <= 0.0D || effMass <= 0.0D || density <= 0.0D) {
            return out.set(0.0D, 0.0D, 0.0D);
        }
        final double driveSpeed = drive.length();
        if (driveSpeed < 1.0E-6D) {
            return out.set(0.0D, 0.0D, 0.0D);
        }
        final double k = 0.5D * density * cD * area * driveSpeed / effMass;
        out.set(drive).mul(k);

        if (maxAccel > 0.0D) {
            final double mag = out.length();
            if (mag > maxAccel) {
                out.mul(maxAccel / mag);
            }
        }
        return out;
    }

    public static Vector3d deltaVelocity(final Vector3d accel, final double dt, final Vector3d out) {
        return out.set(accel).mul(dt);
    }

    public static final double TICK_SECONDS = 0.05D;

    public static final double SYNC_MIN_SPEED = 0.0027621D / TICK_SECONDS;

    public static boolean applyVisibilityFloor(final org.joml.Vector3d vel, final double boostMin) {
        if (boostMin <= 0.0D) {
            return false;
        }
        final double speed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        if (speed < 1.0E-9D) {
            return false;
        }
        if (speed >= boostMin) {
            return false;
        }

        final double k = boostMin / speed;
        vel.x *= k;
        vel.z *= k;
        return true;
    }
}
