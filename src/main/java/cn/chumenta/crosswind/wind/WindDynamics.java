package cn.chumenta.crosswind.wind;

/** 供独立计算使用的世界天气快照。 */
public record WindDynamics(long gameTime, long dayTime, int dimensionSalt,
                           long seed, long previousSeed, double transitionRatio,
                           double rainLevel, double thunderLevel, double pressure,
                           WindSettings settings) {
}
