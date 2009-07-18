/*
 * Copyright 2009 Michael Bedward
 *
 * This file is part of jai-tools.
 *
 * jai-tools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * jai-tools is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with jai-tools.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package jaitools.tiledimage;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests of DiskMemTilesImage: basic tile getting
 * and querying
 *
 * @author Michael Bedward
 * @since 1.0
 * @version $Id$
 */
public class BasicAccessTest extends TiledImageTestBase {

    @Before
    public void setUp() {
        image = makeImage(TILE_WIDTH, XTILES, YTILES);
    }

    @Test
    public void testGetTile() {
        System.out.println("   getTile");
        for (int y = 0; y < YTILES; y++) {
            for (int x = 0; x < XTILES; x++) {
                Raster tile = image.getTile(x, y);
                assertTrue(tile != null);
                assertTrue(tile.getMinX() == x * TILE_WIDTH);
                assertTrue(tile.getMinY() == y * TILE_WIDTH);
            }
        }
    }

    @Test
    public void testGetWritableTile() {
        System.out.println("   getting and releasing writable tiles");
        WritableRaster r = image.getWritableTile(1, 1);
        assertTrue(r != null);
        Rectangle bounds = r.getBounds();
        assertTrue(bounds.x == TILE_WIDTH);
        assertTrue(bounds.y == TILE_WIDTH);

        r = image.getWritableTile(1, 1);
        assertTrue(r == null);

        image.releaseWritableTile(1, 1);
        r = image.getWritableTile(1, 1);
        assertTrue(r != null);
    }


    @Test
    public void testIsTileWritable() {
        System.out.println("   isTileWritable");

        assertFalse(image.isTileWritable(0, 0));

        image.getWritableTile(0, 0);
        assertTrue(image.isTileWritable(0, 0));

        image.releaseWritableTile(0, 0);
        assertFalse(image.isTileWritable(0, 0));
    }


    @Test
    public void testGetWritableTileIndices() {
        System.out.println("   getWritableTileIndices");
        
        Point[] pi = {new Point(0, 0), new Point(XTILES-1, YTILES-1)};

        for (Point p : pi) {
            image.getWritableTile(p.x, p.y);
        }

        Point[] indices = image.getWritableTileIndices();
        assertTrue(indices.length == pi.length);

        boolean[] found = new boolean[indices.length];
        for (Point index : indices) {
            for (int i = 0; i < pi.length; i++) {
                if (index.equals(pi[i])) {
                    found[i] = true;
                    break;
                }
            }
        }

        for (int i = 0; i < found.length; i++) {
            assertTrue(found[i]);
        }
    }


    @Test
    public void testHasTileWriters() {
        System.out.println("   hasTileWriters");

        assertFalse(image.hasTileWriters());

        image.getWritableTile(0, 0);
        assertTrue(image.hasTileWriters());

        image.releaseWritableTile(0, 0);
        assertFalse(image.hasTileWriters());
    }

}