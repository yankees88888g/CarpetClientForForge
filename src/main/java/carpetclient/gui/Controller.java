package carpetclient.gui;

import carpetclient.coders.zerox53ee71ebe11e.Chunkdata;
import carpetclient.coders.zerox53ee71ebe11e.ZeroXstuff;
import carpetclient.pluginchannel.CarpetPluginChannel;
import com.google.common.base.Splitter;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.util.Point;

//import javax.swing.*;
//import java.awt.*;
//import java.awt.event.KeyEvent;
import javax.swing.*;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

public class Controller {
    private GuiChunkGrid debug;
    boolean start = false;
    boolean play = false;
    private boolean live = false;
    private int lastGametick;
    private Point view = new Point();
    private Point dragView = new Point();
    private Point selectionBox;
    private int selectionDimention;

    private Chunkdata.MapView chunkData;
    private Point mouseDown = new Point();
    private boolean panning = false;


    public Controller(GuiChunkGrid d) {
        debug = d;
        lastGametick = 0;
        chunkData = ZeroXstuff.data.getChunkData();
    }

    public boolean startStop() {
        start = !start;

        live = true;
        if (start) {
            home();
            ZeroXstuff.data.clear();
            selectionBox = null;
            play = false;
        }
        PacketBuffer sender = new PacketBuffer(Unpooled.buffer());
        sender.writeInt(CarpetPluginChannel.CHUNK_LOGGER);
        sender.writeBoolean(start); // this is the bit to turn on or off
//        sender.writeBoolean(debug.areStackTracesEnabled()); // this is the bit to send stacktraces
        sender.writeBoolean(true);

        CarpetPluginChannel.packatSender(sender);

        return start;
    }

    public void load() {
        JFrame frame = new JFrame();
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int rval = fc.showOpenDialog(frame);
        if (rval == JFileChooser.APPROVE_OPTION) {
            String path = fc.getSelectedFile().getPath();
            try {
                ObjectInputStream in = new ObjectInputStream(new FileInputStream(path));
                ZeroXstuff.data.readObject(in);
                view.setX(in.readInt());
                view.setY(in.readInt());
                debug.setXText(view.getX());
                debug.setZText(view.getY());
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        setTick(ZeroXstuff.data.getFirstGametick());
    }

    public void save() {
        JFrame frame = new JFrame();
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        int rval = fc.showSaveDialog(frame);
        if (rval == JFileChooser.APPROVE_OPTION) {
            String path = fc.getSelectedFile().getPath();
            try {
                ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(path));
                ZeroXstuff.data.writeObject(out);
                out.writeInt(view.getX());
                out.writeInt(view.getY());
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void back() {
        live = false;
        if (selectionBox != null) {
            setTick(ZeroXstuff.data.getPreviousGametickForChunk(lastGametick, selectionBox.getX(), selectionBox.getY(), selectionDimention));
        } else {
            setTick(ZeroXstuff.data.getPrevGametick(lastGametick));
        }
    }

    public void forward() {
        live = false;
        if (selectionBox != null) {
            setTick(ZeroXstuff.data.getNextGametickForChunk(lastGametick, selectionBox.getX(), selectionBox.getY(), selectionDimention));
        } else {
            setTick(ZeroXstuff.data.getNextGametick(lastGametick));
        }
    }

    public void current() {
        live = true;
        setTick(ZeroXstuff.data.getLastGametick());
    }

    public void comboBoxAction() {
        setTick(lastGametick);
    }

    public void begining() {
        setTick(ZeroXstuff.data.getFirstGametick());
    }

    public void end() {
        setTick(ZeroXstuff.data.getLastGametick());
    }

    public void play() {
        //TODO fix so timer cant go above highest time.
        play = !play;

        if (play) {
            new Timer().start();
        }
    }

    public void home() {
        BlockPos pos = Minecraft.getMinecraft().player.getPosition();
        view.setX(pos.getX() >> 4);
        view.setY(pos.getZ() >> 4);

        debug.setXText(view.getX());
        debug.setZText(view.getY());

        changeDimentionTo(Minecraft.getMinecraft().player.dimension);

        setTick(lastGametick);
    }

    private void changeDimentionTo(int dimension) {
        if (dimension == -1) {
            debug.setSelectedDimension(1);
        } else if (dimension == 1) {
            debug.setSelectedDimension(2);
        } else if (dimension == 0) {
            debug.setSelectedDimension(0);
        }
    }

    public void setTime(String text) {
        //TODO fix so timer cant be set outside bounded time.
        try {
            int gt = Integer.parseInt(text);
            setTick(gt);
        } catch (NumberFormatException e) {
            return;
        }
    }

    public void setX(String text) {
        try {
            int x = Integer.parseInt(text);
            view.setX(x);
        } catch (NumberFormatException e) {
            return;
        }
        setTick(lastGametick);
    }

    public void setZ(String text) {
        try {
            int z = Integer.parseInt(text);
            view.setY(z);
        } catch (NumberFormatException e) {
            return;
        }
        setTick(lastGametick);
    }

    public void liveUpdate(int time) {
        if (!live) return;
        setTick(time);
    }

    void setTick(int gametick) {
        int dimention = debug.getSelectedDimension();
        ChunkGrid canvas = debug.getChunkGrid();
        int sizeX = canvas.sizeX();
        int sizeZ = canvas.sizeZ();

        int minX = view.getX() - sizeX / 2;
        int maxX = view.getX() + sizeX / 2;
        int minZ = view.getY() - sizeZ / 2;
        int maxZ = view.getY() + sizeZ / 2;

        chunkData.seekSpace(dimention, minX, maxX + 2, minZ, maxZ + 2);
        chunkData.seekTime(gametick);

        canvas.clearColors();

        for (Chunkdata.ChunkView cv : chunkData) {
            int color = 0;
            for (int c : cv.getColors()) {
                color = c;
            }

            canvas.setGridColor(cv.getX() - minX, cv.getZ() - minZ, color);
        }

        if (selectionBox != null && selectionDimention == dimention) {
            debug.getChunkGrid().setSelectionBox(selectionBox.getX() - minX, selectionBox.getY() - minZ);
        } else {
            debug.getChunkGrid().setSelectionBox(Integer.MAX_VALUE, 0);
        }

        debug.setTime(gametick);

        lastGametick = gametick;
    }

    public void buttonDown(int x, int y, int button) {
        ChunkGrid canvas = debug.getChunkGrid();
        int dimention = debug.getSelectedDimension();
        int sizeX = canvas.sizeX();
        int sizeZ = canvas.sizeZ();

        int cx = canvas.getGridX(x);
        int cz = canvas.getGridY(y);

        int minX = view.getX() - sizeX / 2;
        int maxX = view.getX() + sizeX / 2;
        int minZ = view.getY() - sizeZ / 2;
        int maxZ = view.getY() + sizeZ / 2;

        if (button == 0) {
            mouseDown.setLocation(x, y);
            dragView.setLocation(view);
        } else if (button == 1) {
            SortedMap<Chunkdata.ChunkLogCoords, Chunkdata.ChunkLogEvent> list = ZeroXstuff.data.getAllLogsForDisplayArea(lastGametick, dimention, minX, maxX, minZ, maxZ);
            for (Map.Entry<Chunkdata.ChunkLogCoords, Chunkdata.ChunkLogEvent> entry : list.entrySet()) {
                Chunkdata.ChunkLogCoords chunk = entry.getKey();
                if (chunk == null) continue;
                Chunkdata.ChunkLogEvent event = list.get(chunk);
                if (event == null) {
                    continue;
                }
                if (chunk.space.x == (cx + minX) && chunk.space.z == (cz + minZ)) {
                    List<String> props = Arrays.asList("Event: " + event.event.toString());
                    String s = ZeroXstuff.data.getStackTraceString(event.stackTraceId);
                    if (s.length() != 0)
                        Minecraft.getMinecraft().displayGuiScreen(new GuiChunkGridChunk(chunk.space.x, chunk.space.z, debug, debug, props, Splitter.onPattern("\\r?\\n").splitToList(s)));
                    else
                        Minecraft.getMinecraft().displayGuiScreen(new GuiChunkGridChunk(chunk.space.x, chunk.space.z, debug, debug, props, null));
                }
            }
        }
    }

    public void buttonUp(int x, int y, int mouseButton) {
        if (mouseButton == 0 && !panning) {
            int cx = debug.getChunkGrid().getGridX(x) + view.getX() - debug.getChunkGrid().sizeX() / 2;
            int cz = debug.getChunkGrid().getGridY(y) + view.getY() - debug.getChunkGrid().sizeZ() / 2;

            if (selectionBox != null && selectionBox.getX() == cx && selectionBox.getY() == cz) {
                selectionBox = null;
                debug.setBackButtonText("Back");
                debug.setForwardButtonText("Forward");
            } else {
                selectionBox = new Point(cx, cz);
                selectionDimention = debug.getSelectedDimension();
                debug.setBackButtonText("Back(chunk)");
                debug.setForwardButtonText("Forward(chunk)");
            }

            setTick(lastGametick);
        }
        panning = false;
    }

    public void mouseDrag(int x, int y, int button) {
        int dx = x - mouseDown.getX();
        int dy = y - mouseDown.getY();
        if (!panning && dx * dx + dy * dy > 5 * 5) {
            panning = true;
        } else if (button == 0 && panning) {
            view.setLocation(dragView.getX() - dx, dragView.getY() - dy);
            debug.setXText(view.getX());
            debug.setZText(view.getY());
            setTick(lastGametick);
        }
    }

    public void scroll(int scrollAmount) {
        ChunkGrid canvas = debug.getChunkGrid();
        canvas.setScale(canvas.width(), canvas.height(), scrollAmount);
        setTick(lastGametick);
    }

    private class Timer extends Thread {

        public void run() {
            while (play) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                setTick(lastGametick + 1);
            }
        }
    }
}