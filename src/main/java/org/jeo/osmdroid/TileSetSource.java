package org.jeo.osmdroid;

import java.io.IOException;
import java.io.InputStream;

import org.jeo.data.TilePyramid;
import org.jeo.data.TileSet;
import org.osmdroid.ResourceProxy;
import org.osmdroid.tileprovider.MapTile;
import org.osmdroid.tileprovider.tilesource.BitmapTileSourceBase.LowMemoryException;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class TileSetSource implements ITileSource {

    static Logger LOG = LoggerFactory.getLogger(TileSetSource.class);

    TileSet tileset;
    TilePyramid tpyr;

    public TileSetSource(TileSet tileset) throws IOException {
        this.tileset = tileset;
        tpyr = tileset.getPyramid();
    }

    @Override
    public int ordinal() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String name() {
        return tileset.getName();
    }

    @Override
    public String localizedName(ResourceProxy proxy) {
        return name();
    }

    @Override
    public String getTileRelativeFilenameString(MapTile aTile) {
        return String.format("%d/%d/%d", aTile.getZoomLevel(), aTile.getX(), aTile.getY());
    }

    @Override
    public Drawable getDrawable(String aFilePath) throws LowMemoryException {
        String[] split = aFilePath.split("/");
        int z = Integer.parseInt(split[0]);
        int x = Integer.parseInt(split[1]);
        int y = Integer.parseInt(split[2]);

        LOG.debug(String.format("request for tile %d, %d, %d", z, x, y));
        return new BitmapDrawable(Bitmap.createBitmap(256, 256, Config.ARGB_8888));
//        // get bbox from tile index
//        Envelope bbox = tpyr.bounds(new Tile(z,x,y,null,null));
//
//        // render
//        Viewport view = map.getView().clone();
//        view.zoomto(bbox);
//
//        Bitmap img = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Config.ARGB_8888);
//        Canvas can = new Canvas(img);
//
//        Renderer r = new Renderer(can);
//        r.init(view);
//        r.render();
//
//        return new BitmapDrawable(img);
    }

    @Override
    public Drawable getDrawable(InputStream aTileInputStream) throws LowMemoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getMinimumZoomLevel() {
        return 0;
    }

    @Override
    public int getMaximumZoomLevel() {
        return tpyr.getGrids().size();
    }

    @Override
    public int getTileSizePixels() {
        return tpyr.getTileWidth();
        //return map.getView().getWidth();
    }
}
