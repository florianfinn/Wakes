import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Defers Wakes' config read by initializing its resolution to the documented default. */
public final class PatchWakes {
    private static final String ENTRY = "com/leclowndu93150/wakes/simulation/WakeHandler.class";
    private static final String MOD_METADATA = "META-INF/neoforge.mods.toml";
    private static final String RESOLUTION = "com/leclowndu93150/wakes/config/enums/Resolution";

    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("Usage: PatchWakes input.jar output.jar GPL-LICENSE.txt");
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        Path license = Path.of(args[2]);
        if (Files.exists(output)) throw new IllegalStateException("Output already exists: " + output);
        boolean found = false;
        try (JarFile source = new JarFile(input.toFile());
             JarOutputStream target = new JarOutputStream(Files.newOutputStream(output))) {
            Enumeration<JarEntry> entries = source.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                byte[] data = source.getInputStream(entry).readAllBytes();
                if (ENTRY.equals(entry.getName())) {
                    data = patch(data);
                    found = true;
                } else if (MOD_METADATA.equals(entry.getName())) {
                    String toml = new String(data, StandardCharsets.UTF_8);
                    if (!toml.contains("license = \"MIT\"")) {
                        throw new IllegalStateException("Expected original MIT metadata");
                    }
                    data = toml.replace("license = \"MIT\"", "license = \"GPL-3.0-only\"")
                            .getBytes(StandardCharsets.UTF_8);
                }
                JarEntry copy = new JarEntry(entry.getName());
                copy.setTime(entry.getTime());
                target.putNextEntry(copy);
                target.write(data);
                target.closeEntry();
            }
            target.putNextEntry(new JarEntry("LICENSE"));
            target.write(Files.readAllBytes(license));
            target.closeEntry();
            target.putNextEntry(new JarEntry("FORK-NOTICE.txt"));
            target.write(("Community fork by florianfinn of Wakes Reforged by Project8gbDeRam, "
                    + "Goby56 and contributors. Fixes early config access in WakeHandler. "
                    + "Source and patcher: https://github.com/florianfinn/Wakes/tree/fix/1.21.1-early-config\n"
                    + "Original: https://www.curseforge.com/minecraft/mc-mods/wakes-reforged\n")
                    .getBytes(StandardCharsets.UTF_8));
            target.closeEntry();
        }
        if (!found) {
            Files.deleteIfExists(output);
            throw new IllegalStateException("WakeHandler class not found");
        }
        System.out.println("Patched: " + output);
    }

    private static byte[] patch(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(reader, 0);
        int[] replaced = {0};
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                if (!"<clinit>".equals(name)) {
                    return super.visitMethod(access, name, descriptor, signature, exceptions);
                }
                replaced[0]++;
                MethodVisitor method = super.visitMethod(access, name, descriptor, signature, exceptions);
                method.visitCode();
                method.visitTypeInsn(Opcodes.NEW, "java/util/HashMap");
                method.visitInsn(Opcodes.DUP);
                method.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false);
                method.visitFieldInsn(Opcodes.PUTSTATIC, "com/leclowndu93150/wakes/simulation/WakeHandler",
                        "INSTANCES", "Ljava/util/Map;");
                method.visitFieldInsn(Opcodes.GETSTATIC, RESOLUTION, "SIXTEEN", "L" + RESOLUTION + ";");
                method.visitFieldInsn(Opcodes.PUTSTATIC, "com/leclowndu93150/wakes/simulation/WakeHandler",
                        "resolution", "L" + RESOLUTION + ";");
                method.visitInsn(Opcodes.ICONST_0);
                method.visitFieldInsn(Opcodes.PUTSTATIC, "com/leclowndu93150/wakes/simulation/WakeHandler",
                        "resolutionResetScheduled", "Z");
                method.visitInsn(Opcodes.RETURN);
                method.visitMaxs(2, 0);
                method.visitEnd();
                return null;
            }
        };
        reader.accept(visitor, 0);
        if (replaced[0] != 1) throw new IllegalStateException("Expected one static initializer");
        return writer.toByteArray();
    }
}
