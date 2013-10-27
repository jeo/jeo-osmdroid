package org.jeo.osmdroid;

import java.io.IOException;
import java.util.List;

import org.jeo.android.graphics.Graphics;
import org.jeo.data.Tile;
import org.jeo.data.TileDataset;
import org.jeo.data.TileGrid;
import org.jeo.data.TilePyramid;
import org.osmdroid.tileprovider.MapTile;
import org.osmdroid.tileprovider.MapTileRequestState;
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class TileSetProvider extends MapTileModuleProviderBase {

    static Logger LOG = LoggerFactory.getLogger(TileSetProvider.class);

    TileDataset tileset;
    TilePyramid tpyr;

    public TileSetProvider(TileDataset tileset) throws IOException {
        super(NUMBER_OF_TILE_DOWNLOAD_THREADS, NUMBER_OF_TILE_FILESYSTEM_THREADS);
        this.tileset = tileset;
        tpyr = tileset.pyramid();

        if (tpyr.getGrids().isEmpty()) {
            throw new IllegalArgumentException("empty tile pyramid");
        }
    }

    @Override
    protected String getName() {
        return "tiles";
    }

    @Override
    protected String getThreadGroupName() {
        return "tiles";
    }

    @Override
    protected Runnable getTileLoader() {
        return new TileLoader() {
            @Override
            protected Drawable loadTile(MapTileRequestState pState) throws CantContinueException {
                MapTile t = pState.getMapTile();
                
                if (LOG.isDebugEnabled()) {
                    LOG.debug("loading tile: " + t);
                }

                int z = t.getZoomLevel();
                TileGrid grid = tpyr.grid(z);

                //TODO: actually check the origin and adjust accordingly
                int y = grid.getHeight() - t.getY() - 1;
                try {
                    Tile tile = tileset.read(z, t.getX(), y);
                    return new BitmapDrawable(Graphics.bitmap(tile));
                } catch (IOException e) {
                    LOG.warn("error loading map tile: " + t, e);
                }
                return null;
            }
        };
    }

    @Override
    public boolean getUsesDataConnection() {
        return false;
    }

    @Override
    public int getMinimumZoomLevel() {
        return tpyr.getGrids().get(0).getZ();
    }

    @Override
    public int getMaximumZoomLevel() {
        List<TileGrid> grids = tpyr.getGrids();
        return grids.get(grids.size()-1).getZ();
    }

    @Override
    public void setTileSource(ITileSource tileSource) {
    }

}
