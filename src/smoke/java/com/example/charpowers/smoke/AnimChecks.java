package com.example.charpowers.smoke;

import com.example.charpowers.CharPowers;
import com.example.charpowers.armour.InvincibleArmour;
import com.example.charpowers.armour.SuitItems;
import com.example.charpowers.client.anim.PlayerAnims;
import com.example.charpowers.lantern.GreenLantern;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

// python tools/run_real_client.py anim: the real game stays open and does what run-real/lab/todo.txt says, one line per step, so poses can be
// looked at from any side without a restart. It writes run-real/lab/done when it has finished a file, and quits on the line "quit" or
// after its time is up. Only in test builds.
//   setup | lantern | suit on/off | gun on/off | fire on/off | key <ability> | pose <name> <tick> | play <name> | clear | cam front/back/left/right/quarter/backquarter/fp
//   pitch <degrees> | yaw <degrees> | crouch on/off | item <id> | reload | wait <ticks> | shot <name> | quit
@Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT)
public final class AnimChecks {
    public static final boolean ON = Boolean.getBoolean("charpowers.animcheck");
    private static final long LIMIT_MS = Long.getLong("charpowers.animlimit", 520_000L);
    private static final Deque<String> LINES = new ArrayDeque<>();
    private static long started;
    private static int wait;
    private static boolean busy;
    private static ArmorStand camera;
    private static String cameraSide = "fp";

    private AnimChecks() {}

    private static Path lab() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("lab");
    }

    private static void server(Consumer<ServerPlayer> what) {
        var server = Minecraft.getInstance().getSingleplayerServer();
        server.execute(() -> {
            try {
                what.accept(server.getPlayerList().getPlayers().get(0));
            } catch (Throwable t) {
                CharPowers.LOG.error("[AnimCheck] step failed", t);
            }
        });
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (!ON || e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getSingleplayerServer() == null || mc.getSingleplayerServer().getPlayerList().getPlayers().isEmpty()) return;
        if (started == 0) started = System.currentTimeMillis();
        if (System.currentTimeMillis() - started > LIMIT_MS) {
            CharPowers.LOG.info("[AnimCheck] time is up");
            mc.stop();
            return;
        }
        // nothing until the world is really on screen: the first frames are still "Loading terrain"
        if (mc.screen != null || mc.level == null || mc.levelRenderer.countRenderedChunks() == 0) return;
        placeCamera(mc);
        if (wait > 0) {
            wait--;
            return;
        }
        try {
            if (LINES.isEmpty()) {
                if (busy) {
                    busy = false;
                    Files.writeString(lab().resolve("done"), "done");
                }
                Path todo = lab().resolve("todo.txt");
                if (!Files.exists(todo)) return;
                List<String> lines = Files.readAllLines(todo);
                Files.delete(todo);
                Files.deleteIfExists(lab().resolve("done"));
                LINES.addAll(lines);
                busy = true;
            }
            while (!LINES.isEmpty() && wait == 0) run(mc, LINES.poll().trim());
        } catch (Throwable t) {
            CharPowers.LOG.error("[AnimCheck] line failed", t);
        }
    }

    private static void run(Minecraft mc, String line) {
        if (line.isEmpty() || line.startsWith("#")) return;
        String[] w = line.split("\\s+");
        LocalPlayer me = mc.player;
        switch (w[0]) {
            case "setup" -> server(p -> {
                ServerLevel level = p.serverLevel();
                p.setGameMode(GameType.CREATIVE);
                p.getAbilities().flying = false;
                level.setDayTime(6000);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
                p.getInventory().clearContent();
            });
            case "lantern" -> server(p -> {
                GreenLantern.POWER.give(p);
                GreenLantern.POWER.data(p).putDouble("battery", 100);
            });
            case "battery" -> server(p -> GreenLantern.POWER.data(p).putDouble("battery", 100));
            case "suit" -> server(p -> {
                if ("on".equals(w[1])) {
                    InvincibleArmour.POWER.give(p);
                    p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(SuitItems.SUIT.get()));
                } else {
                    p.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
                }
            });
            case "gun" -> server(p -> GreenLantern.POWER.onKey(p, "minigun"));
            case "fire" -> server(p -> {
                GreenLantern.POWER.data(p).putDouble("battery", 100);
                GreenLantern.POWER.onClientAction(p, "on".equals(w[1]) ? "fire:1" : "fire:0");
            });
            case "key" -> server(p -> GreenLantern.POWER.onKey(p, w[1]));
            case "pose" -> {
                if (!PlayerAnims.freeze(me.getId(), w[1], Double.parseDouble(w[2]))) CharPowers.LOG.warn("[AnimCheck] no pose {}", w[1]);
            }
            case "play" -> PlayerAnims.play(me.getId(), w[1]);
            case "clear" -> PlayerAnims.clear(me.getId());
            case "reload" -> {
                String problem = PlayerAnims.reload();
                if (problem != null) CharPowers.LOG.warn("[AnimCheck] {}", problem);
            }
            case "cam" -> cameraSide = w[1];
            case "pitch" -> {
                me.setXRot(Float.parseFloat(w[1]));
                me.xRotO = me.getXRot();
            }
            case "yaw" -> {
                float yaw = Float.parseFloat(w[1]);
                me.setYRot(yaw);
                me.yRotO = yaw;
                me.setYHeadRot(yaw);
                me.yHeadRotO = yaw;
                if (w.length < 3) {
                    me.yBodyRot = yaw;
                    me.yBodyRotO = yaw;
                }
            }
            case "crouch" -> me.setShiftKeyDown("on".equals(w[1]));
            case "item" -> server(p -> {
                var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(w[1]));
                p.getInventory().setItem(p.getInventory().selected, new ItemStack(item));
            });
            case "wait" -> wait = Integer.parseInt(w[1]);
            case "shot" -> {
                Screenshot.grab(mc.gameDirectory, w[1] + ".png", mc.getMainRenderTarget(), c -> {});
            }
            case "quit" -> mc.stop();
            default -> CharPowers.LOG.warn("[AnimCheck] what is {}", line);
        }
    }

    // Keeps a camera at a fixed spot around the player, looking at their chest, so every picture of a pose is taken from the same place.
    private static void placeCamera(Minecraft mc) {
        LocalPlayer me = mc.player;
        // the game's own third-person cameras, the way the walk-through and a player see it
        if ("tpback".equals(cameraSide) || "tpfront".equals(cameraSide)) {
            if (mc.getCameraEntity() != me) mc.setCameraEntity(me);
            mc.options.setCameraType("tpback".equals(cameraSide) ? CameraType.THIRD_PERSON_BACK : CameraType.THIRD_PERSON_FRONT);
            mc.options.hideGui = true;
            return;
        }
        if ("fp".equals(cameraSide)) {
            if (mc.getCameraEntity() != me) mc.setCameraEntity(me);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.options.hideGui = false;
            return;
        }
        if (camera == null || camera.level() != mc.level) camera = new ArmorStand(EntityType.ARMOR_STAND, mc.level);
        double around = switch (cameraSide) {
            case "front" -> 0;
            case "left" -> 90;
            case "back" -> 180;
            case "right" -> -90;
            case "quarter" -> -40;
            case "backquarter" -> -140;
            default -> 0;
        };
        double distance = 3.4;
        double rad = Math.toRadians(me.yBodyRot + around);
        Vec3 chest = me.position().add(0, 1.0, 0);
        // yaw 0 faces +z, so the spot in front of them is along their facing
        Vec3 at = chest.add(-Math.sin(rad) * distance, 0.25, Math.cos(rad) * distance);
        Vec3 look = chest.subtract(at).normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
        float pitch = (float) -Math.toDegrees(Math.asin(look.y));
        double eye = camera.getEyeHeight();
        camera.setPos(at.x, at.y - eye, at.z);
        camera.xo = camera.xOld = at.x;
        camera.yo = camera.yOld = at.y - eye;
        camera.zo = camera.zOld = at.z;
        // a living thing's view follows its head
        camera.setYRot(yaw);
        camera.yRotO = yaw;
        camera.setYHeadRot(yaw);
        camera.yHeadRotO = yaw;
        camera.setXRot(pitch);
        camera.xRotO = pitch;
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        mc.options.hideGui = true;
        if (mc.getCameraEntity() != camera) mc.setCameraEntity(camera);
    }
}
