package org.jeo.osmdroid;

import org.jeo.android.graphics.Graphics;
import org.jeo.data.Query;
import org.jeo.data.VectorDataset;
import org.jeo.feature.Feature;
import org.jeo.geom.CoordinatePath;
import org.jeo.geom.Geom;
import org.jeo.map.Rule;
import org.jeo.map.RuleList;
import org.jeo.map.Style;
import org.jeo.proj.Proj;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateFilter;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;

import static org.jeo.android.graphics.Graphics.*;
import static org.jeo.map.CartoCSS.MARKER_HEIGHT;
import static org.jeo.map.CartoCSS.MARKER_WIDTH;

public class VectorOverlay extends Overlay {

    static Logger LOG = LoggerFactory.getLogger(VectorOverlay.class);

    /** the dataset to overlay */
    VectorDataset data;

    /** style for rendered vectors */
    Style style;

    /** intermediate drawing variables */
    PointF p;
    Rect r;

    public VectorOverlay(Context context, VectorDataset data, Style style) {
        super(context);
        this.data = data;
        this.style = style;

        p = new PointF();
        r = new Rect(); 
    }

    @Override
    protected void draw(Canvas c, MapView mapView, boolean shadow) {
        if (shadow) {
            return;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Rendering [%s] at level %d", 
                mapView.getBoundingBox().toString(), mapView.getZoomLevel()));
        }
        DrawContext context = new DrawContext(mapView);

        RuleList rules = style.getRules().selectById(data.getName(), true).flatten();

        Query q = new Query();
        q.bounds(context.bbox).reproject(Proj.EPSG_4326);

        try {
            for (Feature f : data.cursor(q)) {
                draw(f, rules, c, context);
            }
        }
        catch(Exception e) {
            LOG.warn("failure rendering overlay", e);
        }
    }

    void draw(Feature f, RuleList rules, Canvas canvas, DrawContext ctx) {
        Geometry g = f.geometry();
        if (g == null) {
            // nothing to do for this feature
            return;
        }

        // compile the rules for this feature
        Rule rule = rules.match(f).collapse();

        draw(g, f, rule, canvas, ctx);
    }

    void draw(Geometry g, Feature f, Rule rule, Canvas canvas, DrawContext ctx) {
        // draw
        switch(Geom.Type.from(g)) {
        case POINT:
        case MULTIPOINT:
            drawPoint(g, f, rule, canvas, ctx);
            break;
        case LINESTRING:
        case MULTILINESTRING:
            drawLine(g, f, rule, canvas, ctx);
            break;
        case POLYGON:
        case MULTIPOLYGON:
            drawPolygon(g, f, rule, canvas, ctx);
            break;
        case GEOMETRYCOLLECTION:
            for (Geometry h : Geom.iterate((GeometryCollection)g)) {
                draw(h, f, rule, canvas, ctx);
            }
            break;
        default:
            LOG.warn("feature geometry not supported: " + g);
        }
    }

    void drawPoint(Geometry g, Feature f, Rule rule, Canvas canvas, DrawContext ctx) {
        Paint fill = markFillPaint(f, rule);
        Paint line = markLinePaint(f, rule);

        float width = rule.number(f, MARKER_WIDTH, 10f);
        float height = rule.number(f, MARKER_HEIGHT, width);

        if (fill != null) {
            CoordinatePath path = path(g, ctx);
            while(path.hasNext()) {
                Coordinate c = path.next();
                canvas.drawOval(rectFromCenter(point(c,p), width, height), fill);
            }
        }

        if (line != null) {
            CoordinatePath path = path(g, ctx);
            while(path.hasNext()) {
                Coordinate c = path.next();
                canvas.drawOval(rectFromCenter(point(c,p), width, height), line);
            }
        }
    }

    void drawLine(Geometry g, Feature f, Rule rule, Canvas canvas, DrawContext ctx) {
       canvas.drawPath(Graphics.path(path(g, ctx)), linePaint(f, rule, null));
    }

    void drawPolygon(Geometry g, Feature f, Rule rule, Canvas canvas, DrawContext ctx) {
        Paint fill = polyFillPaint(f, rule);
        Paint line = polyLinePaint(f, rule, null);

        Path p = Graphics.path(path(g, ctx));
        if (fill != null) {
            canvas.drawPath(p, fill);
        }

        if (line != null) {
            //p.rewind();
            canvas.drawPath(p, line);
        }
    }

    CoordinatePath path(Geometry g, DrawContext ctx) {
        //TODO: clip
        return CoordinatePath.create(g)
            .transform(transform(ctx)).generalize(ctx.xpixsize, ctx.ypixsize);
    }

    CoordinateFilter transform(final DrawContext ctx) {
        return new CoordinateFilter() {
            android.graphics.Point p = new Point();
            GeoPoint gp = new GeoPoint(0, 0);

            @Override
            public void filter(Coordinate c) {
                gp.setLatitudeE6((int)(c.y*1E6));
                gp.setLongitudeE6((int)(c.x*1E6));

                ctx.mapView.getProjection().toMapPixels(gp, p);

                c.x = p.x;
                c.y = p.y;
            }
        };
    }

    class DrawContext {

        /**
         * the map view
         */
        MapView mapView;
        
        /**
         * screen dimensions
         */
        Rect screenSize;

        /**
         * the bounding box in world coordinates
         */
        Envelope bbox;

        /**
         * pixel dimensions in world coordinates
         */
        double xpixsize;
        double ypixsize;

        DrawContext(MapView mapView) {
            this.mapView = mapView;
            
            screenSize = new Rect();
            mapView.getScreenRect(screenSize);

            bbox = envelope(mapView.getBoundingBox());
            xpixsize = bbox.getWidth() / screenSize.width();
            ypixsize = bbox.getHeight() / screenSize.height();
        }

        Envelope envelope(BoundingBoxE6 bbox) {
            double x1 = bbox.getLonWestE6() / ((double)1E6);
            double y1 = bbox.getLatSouthE6() / ((double)1E6);
            double x2 = bbox.getLonEastE6()/ ((double)1E6);
            double y2 = bbox.getLatNorthE6() / ((double)1E6);

            return new Envelope(x1, x2, y1, y2);
        }
    }
}
