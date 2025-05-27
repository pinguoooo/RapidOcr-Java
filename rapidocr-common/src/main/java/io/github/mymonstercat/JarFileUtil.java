package io.github.mymonstercat;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * @author Monster
 */
@Slf4j
public class JarFileUtil {

    public static final String TEMP_DIR = new File(Objects.toString(System.getProperty("java.io.tmpdir")), "ocrJava").getPath();

    private JarFileUtil() {
    }

    static File tempDir = null;

    /**
     * @param filePath     resources下的文件路径
     * @param aimDir
     * @param load
     * @param deleteOnExit
     * @throws IOException
     */
    public static synchronized void copyFileFromJar(String filePath, String aimDir, boolean load, boolean deleteOnExit) throws IOException {
        System.out.println("正在加载文件：" + filePath);
        // 获取文件名并校验
        String[] parts = filePath.split("/");
        String filename = (parts.length > 1) ? parts[parts.length - 1] : null;
        if (filename == null || filename.length() < 3) {
            throw new IllegalArgumentException("文件名必须至少有3个字符长.");
        }
        // 检查目标文件夹是否存在
        if (tempDir == null) {
            tempDir = new File(TEMP_DIR, aimDir);
            if (!tempDir.exists() && !tempDir.mkdirs()) {
                throw new IOException("无法在临时目录创建文件夹" + tempDir);
            }
        }
        // 在临时文件夹下创建文件
        File temp = new File(tempDir, filename.startsWith("/") ? filename : "/" + filename);
        if (!temp.exists() || !checkFileArch(temp.getAbsolutePath())) {
            System.out.println("正在加载文件" + filePath + "到" + temp.getAbsolutePath());

            // 从jar包中复制文件到系统临时文件夹
            try (InputStream is = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(filePath)) {
                if (is != null) {
                    Files.copy(is, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    throw new NullPointerException();
                }
            } catch (IOException e) {
                Files.delete(temp.toPath());
                throw new IOException("无法复制文件 " + filePath + " 到 " + temp.getAbsolutePath(), e);
            } catch (NullPointerException e) {
                throw new FileNotFoundException("文件 " + filePath + " 在JAR中未找到.");
            }
        }
        // 加载临时文件夹中的动态库
        if (load) {
            System.load(temp.getAbsolutePath());
        }
        // JVM结束时删除临时文件和临时文件夹
        if (deleteOnExit) {
            temp.deleteOnExit();
            tempDir.deleteOnExit();
        }
        log.debug("将文件{}复制到{}，加载此文件：{}，JVM退出时删除此文件：{}", filePath, aimDir, load, deleteOnExit);
    }

    /**
     * 验证已存在的文件是否为当前运行的架构
     * 主要是检查 mac 下，Apple Silicon 芯片，使用 rosetta 运行 x86_64 JDK，则文件需要为x86_64，而不是arm64
     *
     * @return
     */
    private static boolean checkFileArch(String fileName) {
        final String OS_NAME = System.getProperty("os.name").toLowerCase();
        final String OS_ARCH = System.getProperty("os.arch").toLowerCase();

        // 提前返回 true，简化逻辑结构
        if (!OS_NAME.contains("mac") || !fileName.endsWith(".dylib")) {
            return true;
        }
        System.out.println("正在验证文件：" + fileName + " 的架构是否匹配当前运行环境：" + OS_NAME + " " + OS_ARCH);

        try {
            // 示例：ProcessBuilder 执行 file 命令并解析输出
            Process process = new ProcessBuilder("file", fileName).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output = reader.readLine();
            System.out.println("文件输出：" + output);
            // 解析 output 判断架构是否匹配 OS_ARCH

            boolean isMatch = false;
            if (OS_ARCH.contains("x86_64") && output.contains("x86_64")) {
                isMatch = true;
            } else if (OS_ARCH.contains("arch64") && output.contains("arm64")) {
                isMatch = true;
            }

            System.out.println("文件架构" + (isMatch ? "" : "不") + "匹配当前运行环境：" + OS_NAME + " " + OS_ARCH);
            return isMatch;
        } catch (Exception e) {
            System.err.println("执行文件命令失败: " + e.getMessage());
            // 可选：throw new RuntimeException("无法验证 dylib 架构", e);
            return false;
        }
    }

    /**
     * 从jar包中复制models文件夹下的内容
     *
     * @param model 要使用的模型
     */
    public static void copyModelsFromJar(Model model, boolean isDelOnExit) throws IOException {
        String modelsDir = model.getModelsDir();
        String noStart = modelsDir.startsWith("/") ? modelsDir.substring(1, modelsDir.length()) : modelsDir;
        String base = noStart.endsWith("/") ? noStart : noStart + "/";
        for (final String path : model.getModelFileArray()) {
            copyFileFromJar(base + path, "/" + model.getModelType(), Boolean.FALSE, isDelOnExit);
        }
    }
}
