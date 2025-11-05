package Perdume.rpg.core.util;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 인벤토리의 내용을 Base64 문자열로 변환하거나,
 * Base64 문자열을 인벤토리 내용으로 복원하는 유틸리티 클래스입니다.
 */
public class InventorySerializer {

    /**
     * 인벤토리의 전체 내용을 Base64 문자열로 변환합니다.
     * @param inventory 직렬화할 인벤토리
     * @return Base64로 인코딩된 문자열
     * @throws IllegalStateException 변환 중 오류 발생 시
     */
    public static String inventoryToBase64(Inventory inventory) throws IllegalStateException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            // 인벤토리 크기를 먼저 저장하여 나중에 검증할 수 있도록 합니다.
            dataOutput.writeInt(inventory.getSize());

            // 각 슬롯의 아이템을 스트림에 씁니다.
            for (int i = 0; i < inventory.getSize(); i++) {
                dataOutput.writeObject(inventory.getItem(i));
            }

            dataOutput.close();
            // 바이트 배열을 Base64 문자열로 인코딩하여 반환합니다.
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("인벤토리를 저장할 수 없습니다.", e);
        }
    }

    /**
     * Base64 문자열로부터 아이템들을 읽어와 기존 인벤토리에 덮어씌웁니다.
     * @param data Base64로 인코딩된 문자열
     * @param inventory 아이템을 복원할 인벤토리
     * @throws IOException 데이터 읽기 중 오류 발생 시
     */
    public static void inventoryFromBase64(String data, Inventory inventory) throws IOException {
        if (data == null || data.isEmpty()) {
            return;
        }
        try {
            // Base64 문자열을 디코딩하여 바이트 배열로 변환합니다.
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            
            // 복원 전, 현재 인벤토리의 내용을 모두 비웁니다.
            inventory.clear();

            // 저장된 인벤토리 크기를 읽어옵니다.
            int size = dataInput.readInt();
            
            // 저장된 크기와 현재 인벤토리의 크기가 다르면, 데이터 손상을 방지하기 위해 로드를 중단합니다.
            if(size != inventory.getSize()){
                dataInput.close();
                return;
            }

            // 각 슬롯에 아이템을 다시 채워 넣습니다.
            for (int i = 0; i < size; i++) {
                inventory.setItem(i, (ItemStack) dataInput.readObject());
            }

            dataInput.close();
        } catch (ClassNotFoundException e) {
            throw new IOException("ItemStack 클래스를 찾을 수 없어 인벤토리를 불러올 수 없습니다.", e);
        }
    }
}
