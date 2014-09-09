/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.smasher;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

/**
 *
 * @author ucchy
 */
public class SmasherConfig {

    private Smasher parent;

    private Material smasherMaterial;
    private int durabilityCost;
    private boolean enableCraft;

    /**
     * コンストラクタ
     * @param parent
     */
    public SmasherConfig(Smasher parent) {
        this.parent = parent;
        reloadConfig();
    }

    /**
     * コンフィグを読み込む
     */
    protected void reloadConfig() {

        if ( !parent.getDataFolder().exists() ) {
            parent.getDataFolder().mkdirs();
        }

        File file = new File(parent.getDataFolder(), "config.yml");
        if ( !file.exists() ) {
            String configFileName = "config_en.yml";
            if ( System.getProperty("user.language").equals("ja") ) {
                configFileName = "config_ja.yml";
            }
            copyFileFromJar(parent.getJarFile(), file, configFileName, false);
        }

        parent.reloadConfig();
        FileConfiguration conf = parent.getConfig();

        String materialTemp = conf.getString("smasherMaterial", "STICK");
        smasherMaterial = Material.matchMaterial(materialTemp);
        if ( smasherMaterial == null ) {
            smasherMaterial = Material.STICK;
        }

        durabilityCost = conf.getInt("durabilityCost", 2);
        if ( durabilityCost < 0 ) {
            durabilityCost = 0;
        }

        enableCraft = conf.getBoolean("enableCraft", true);
    }

    /**
     * jarファイルの中に格納されているファイルを、jarファイルの外にコピーするメソッド
     * @param jarFile jarファイル
     * @param targetFile コピー先
     * @param sourceFilePath コピー元
     * @param isBinary バイナリファイルかどうか
     */
    private static void copyFileFromJar(
            File jarFile, File targetFile, String sourceFilePath, boolean isBinary) {

        InputStream is = null;
        FileOutputStream fos = null;
        BufferedReader reader = null;
        BufferedWriter writer = null;

        File parent = targetFile.getParentFile();
        if ( !parent.exists() ) {
            parent.mkdirs();
        }

        try {
            JarFile jar = new JarFile(jarFile);
            ZipEntry zipEntry = jar.getEntry(sourceFilePath);
            is = jar.getInputStream(zipEntry);

            fos = new FileOutputStream(targetFile);

            if ( isBinary ) {
                byte[] buf = new byte[8192];
                int len;
                while ( (len = is.read(buf)) != -1 ) {
                    fos.write(buf, 0, len);
                }
                fos.flush();
                fos.close();
                is.close();

            } else {
                reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                writer = new BufferedWriter(new OutputStreamWriter(fos));

                String line;
                while ((line = reader.readLine()) != null) {
                    writer.write(line);
                    writer.newLine();
                }

            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if ( writer != null ) {
                try {
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                    // do nothing.
                }
            }
            if ( reader != null ) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // do nothing.
                }
            }
            if ( fos != null ) {
                try {
                    fos.flush();
                    fos.close();
                } catch (IOException e) {
                    // do nothing.
                }
            }
            if ( is != null ) {
                try {
                    is.close();
                } catch (IOException e) {
                    // do nothing.
                }
            }
        }
    }

    /**
     * @return smasherMaterial
     */
    public Material getSmasherMaterial() {
        return smasherMaterial;
    }

    /**
     * @return durabilityCost
     */
    public int getDurabilityCost() {
        return durabilityCost;
    }

    /**
     * @return enableCraft
     */
    public boolean isEnableCraft() {
        return enableCraft;
    }
}
