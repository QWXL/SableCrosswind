本目录里的 instruments.nbt 是思索场景的"舞台"（Ponder 只负责加载它）。

它不由代码生成，而是用【原版结构方块】在开发客户端里导出的：
  1. 按 docs/08-思索场景.md §5 施工（局部 y=0 底座 7×7、y=1 立柱、y=2 三个仪表）；
  2. 结构方块（默认是 LOAD 模式，要先切 SAVE）放在底座那层的正下方一角，
     Relative Position 保持默认 (0,1,0)、Size 填 (7,3,7)，存名 instruments；
  3. 文件落在 <世界存档>/generated/minecraft/structures/instruments.nbt，
     复制到这里并改名为 instruments.nbt。

两件容易忘的事：
  · 必须是 GZIP 压缩的原版结构格式（PonderSceneRegistry 用 GZIPInputStream 读）；
  · 框的下边界必须正好是底座那一层（showBasePlate() 取的就是局部 y=0）。

改完请跑一遍机械核验（它会从 Java 源码解析期望坐标，逐格对照）：
  python tools/verify_ponder_schematic.py
