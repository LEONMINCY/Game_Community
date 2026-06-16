package com.sys.pro.utils;

import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 统一解析上传目录，兼容早期项目根目录下的 files 目录和近期 uploads 目录。
 */
public final class UploadPathResolver {
    /**
     * 路径解析工具只暴露静态方法，不允许创建实例。
     */
    private UploadPathResolver() {
    }



    /**
     * 完成文件上传中的 resolvePrimaryUploadDir 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param uploadPath uploadPath 字段，来源于当前接口入参或内部调用上下文。
     * @return 文件上传在该步骤产出的业务结果。
     */
    public static Path resolvePrimaryUploadDir(String uploadPath) {
        Path configured = normalize(uploadPath);
        if (StringUtils.hasText(System.getenv("APP_UPLOAD_PATH"))) {
            return configured;
        }
        return legacyFileDirs().stream()
                .filter(Files::isDirectory)
                .findFirst()
                .orElse(configured);
    }

    /**
     * 完成文件上传中的 resolveReadableUploadDirs 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param uploadPath uploadPath 字段，来源于当前接口入参或内部调用上下文。
     * @return 文件上传列表数据。
     */
    public static List<Path> resolveReadableUploadDirs(String uploadPath) {
        Set<String> seen = new LinkedHashSet<>();
        List<Path> dirs = new ArrayList<>();
        addDir(dirs, seen, resolvePrimaryUploadDir(uploadPath), true);
        addDir(dirs, seen, normalize(uploadPath), true);
        legacyFileDirs().forEach(path -> addDir(dirs, seen, path, false));
        legacyUploadDirs().forEach(path -> addDir(dirs, seen, path, false));
        return dirs;
    }

    /**
     * 完成文件上传中的 addDir 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param dirs dirs 字段，来源于当前接口入参或内部调用上下文。
     * @param seen seen 字段，来源于当前接口入参或内部调用上下文。
     * @param path path 字段，来源于当前接口入参或内部调用上下文。
     * @param keepEvenMissing keepEvenMissing 字段，来源于当前接口入参或内部调用上下文。
     */
    private static void addDir(List<Path> dirs, Set<String> seen, Path path, boolean keepEvenMissing) {
        if (path == null) {
            return;
        }
        Path normalized = path.toAbsolutePath().normalize();
        if ((keepEvenMissing || Files.isDirectory(normalized)) && seen.add(normalized.toString())) {
            dirs.add(normalized);
        }
    }

    /**
     * 完成文件上传中的 normalize 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param uploadPath uploadPath 字段，来源于当前接口入参或内部调用上下文。
     * @return 文件上传在该步骤产出的业务结果。
     */
    private static Path normalize(String uploadPath) {
        String path = StringUtils.hasText(uploadPath) ? uploadPath : "./files";
        return Paths.get(path).toAbsolutePath().normalize();
    }

    /**
     * 完成文件上传中的 legacyFileDirs 步骤，保证该环节的数据和状态可以继续向下流转。
     * @return 文件上传列表数据。
     */
    private static List<Path> legacyFileDirs() {
        return Arrays.asList(
                Paths.get("D:", "Game_Community", "game-community-website", "game-community-website", "files").toAbsolutePath().normalize(),
                Paths.get("D:", "Game_Community", "files").toAbsolutePath().normalize(),
                Paths.get("D:", "files").toAbsolutePath().normalize(),
                Paths.get("D:", "upload").toAbsolutePath().normalize(),
                Paths.get("game-community-website", "game-community-website", "files").toAbsolutePath().normalize(),
                Paths.get("files").toAbsolutePath().normalize(),
                Paths.get("..", "files").toAbsolutePath().normalize(),
                Paths.get("..", "..", "files").toAbsolutePath().normalize(),
                Paths.get("..", "..", "..", "files").toAbsolutePath().normalize()
        );
    }

    /**
     * 完成文件上传中的 legacyUploadDirs 步骤，保证该环节的数据和状态可以继续向下流转。
     * @return 文件上传列表数据。
     */
    private static List<Path> legacyUploadDirs() {
        return Arrays.asList(
                Paths.get("D:", "Game_Community", "uploads").toAbsolutePath().normalize(),
                Paths.get("D:", "uploads").toAbsolutePath().normalize(),
                Paths.get("uploads").toAbsolutePath().normalize(),
                Paths.get("..", "uploads").toAbsolutePath().normalize(),
                Paths.get("..", "..", "uploads").toAbsolutePath().normalize(),
                Paths.get("..", "..", "..", "uploads").toAbsolutePath().normalize(),
                Paths.get("game-community-website", "game-community-website", "uploads").toAbsolutePath().normalize()
        );
    }
}
