/*
 * $Id$
 *
 * This file is part of the iText (R) project.
 * Copyright (c) 1998-2014 iText Group NV
 * Authors: Bruno Lowagie, Paulo Soares, et al.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3
 * as published by the Free Software Foundation with the addition of the
 * following permission added to Section 15 as permitted in Section 7(a):
 * FOR ANY PART OF THE COVERED WORK IN WHICH THE COPYRIGHT IS OWNED BY
 * ITEXT GROUP. ITEXT GROUP DISCLAIMS THE WARRANTY OF NON INFRINGEMENT
 * OF THIRD PARTY RIGHTS
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, see http://www.gnu.org/licenses or write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA, 02110-1301 USA, or download the license from the following URL:
 * http://itextpdf.com/terms-of-use/
 *
 * The interactive user interfaces in modified source and object code versions
 * of this program must display Appropriate Legal Notices, as required under
 * Section 5 of the GNU Affero General Public License.
 *
 * In accordance with Section 7(b) of the GNU Affero General Public License,
 * a covered work must retain the producer line in every PDF that is created
 * or manipulated using iText.
 *
 * You can be released from the requirements of the license by purchasing
 * a commercial license. Buying such a license is mandatory as soon as you
 * develop commercial activities involving the iText software without
 * disclosing the source code of your own applications.
 * These activities include: offering paid services to customers as an ASP,
 * serving PDFs on the fly in a web application, shipping iText with a closed
 * source product.
 *
 * For more information, please contact iText Software Corp. at this
 * address: sales@itextpdf.com
 */
package com.itextpdf.basics.font.otf;

import com.itextpdf.basics.IntHashtable;
import com.itextpdf.basics.io.RandomAccessFileOrArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author <a href="mailto:paawak@gmail.com">Palash Ray</a>
 */
public abstract class OpenTypeFontTableReader {

	protected final RandomAccessFileOrArray rf;
	protected final int tableLocation;
	
    protected List<OpenTableLookup> lookupList;
    protected OpenTypeScript scriptsType;
    protected OpenTypeFeature featuresType;
    private final Map<Integer, Glyph> indexGlyphMap;
    private final OpenTypeGdefTableReader gdef;

	public OpenTypeFontTableReader(RandomAccessFileOrArray rf, int tableLocation, OpenTypeGdefTableReader gdef,
                                   Map<Integer, Glyph> indexGlyphMap) throws IOException {
		this.rf = rf;
		this.tableLocation = tableLocation;
        this.indexGlyphMap = indexGlyphMap;
        this.gdef = gdef;
	}
	
    protected int getGlyphWidth(int index) {
        Glyph glyph = indexGlyphMap.get(index);
        if (glyph == null) {
            return 0;
        } else {
            return glyph.width;
        }
    }
    
    public Integer getGlyphToCharacter(int index) {
        Glyph glyph = indexGlyphMap.get(index);
        if (glyph == null) {
            return null;
        } else {
            return glyph.unicode;
        }
    }
        
	/**
	 * This is the starting point of the class. A sub-class must call this
	 * method to start getting call backs to the {@link #readLookupTable(int, int, int[])}
	 * method.
	 * @throws FontReadingException 
	 */
	protected final void startReadingTable() throws FontReadingException {
		try {
            rf.seek(tableLocation);
            /*int version =*/ rf.readInt(); //version not used
            int scriptListOffset = rf.readUnsignedShort();
            int featureListOffset = rf.readUnsignedShort();
            int lookupListOffset = rf.readUnsignedShort();
			// read the Script tables
            scriptsType = new OpenTypeScript(this, tableLocation + scriptListOffset);
			// read Feature table
            featuresType = new OpenTypeFeature(this, tableLocation + featureListOffset);
			// read LookUpList table
			readLookupListTable(tableLocation + lookupListOffset);
		} catch (IOException e) {
			throw new FontReadingException("Error reading font file", e);
		}
	}
    
    public OpenTableLookup getLookupTable(int idx) {
        if (idx < 0 || idx >= lookupList.size()) {
            return null;
        }
        return lookupList.get(idx);
    }

    public List<ScriptRecord> getScriptRecords() {
        return scriptsType.getScriptRecords();
    }
    
    public List<FeatureRecord> getFeatureRecords() {
        return featuresType.getRecords();
    }
    
    public List<FeatureRecord> getFeatures(String[] scripts, String language) {
        LanguageRecord rec = scriptsType.getLanguageRecord(scripts, language);
        if (rec == null) {
            return null;
        }
        ArrayList<FeatureRecord> ret = new ArrayList<FeatureRecord>();
        for (int f : rec.features) {
            ret.add(featuresType.getRecord(f));
        }
        return ret;
    }
         
    public List<FeatureRecord> getSpecificFeatures(List<FeatureRecord> features, String[] specific) {
        if (specific == null) {
            return features;
        }
        HashSet<String> hs = new HashSet<String>();
        Collections.addAll(hs, specific);
        List<FeatureRecord> recs = new ArrayList<FeatureRecord>();
        for (FeatureRecord rec : features) {
            if (hs.contains(rec.tag)) {
                recs.add(rec);
            }
        }
        return recs;
    }
    
    public FeatureRecord getRequiredFeature(String[] scripts, String language) {
        LanguageRecord rec = scriptsType.getLanguageRecord(scripts, language);
        if (rec == null)
            return null;
        return featuresType.getRecord(rec.featureRequired);
    }
            
    public List<OpenTableLookup> getLookups(FeatureRecord[] features) {
        IntHashtable hash = new IntHashtable();
        for (FeatureRecord rec : features) {
            for (int idx : rec.lookups) {
                hash.put(idx, 1);
            }
        }
        ArrayList<OpenTableLookup> ret = new ArrayList<OpenTableLookup>();
        for (int idx : hash.toOrderedKeys()) {
            ret.add(lookupList.get(idx));
        }
        return ret;
    }
    
    public boolean IsSkip(int glyph, int flag) {
        return gdef.IsSkip(glyph, flag);
    }
    
	protected abstract OpenTableLookup readLookupTable(int lookupType, int lookupFlag, int[] subTableLocations)
			throws IOException;

	private void readLookupListTable(int lookupListTableLocation)
			throws IOException {
        lookupList = new ArrayList<>();
		rf.seek(lookupListTableLocation);
		int lookupCount = rf.readUnsignedShort();
		int[] lookupTableLocations = readUShortArray(lookupCount, lookupListTableLocation);
		// read LookUp tables
        for (int lookupLocation : lookupTableLocations) {
            readLookupTable(lookupLocation);
        }
	}

	private void readLookupTable(int lookupTableLocation) throws IOException {
		rf.seek(lookupTableLocation);
		int lookupType = rf.readUnsignedShort();
		int lookupFlag = rf.readUnsignedShort();
		int subTableCount = rf.readUnsignedShort();
		int[] subTableLocations = readUShortArray(subTableCount, lookupTableLocation);
        lookupList.add(readLookupTable(lookupType, lookupFlag, subTableLocations));
	}

    protected final OtfClass readClassDefinition(int classLocation) throws IOException {
        return new OtfClass(rf, classLocation);
    }
    
    protected final int[] readUShortArray(int size, int location) throws IOException {
        return OtfReadCommon.readUShortArray(rf, size, location);
    }
    
    protected final int[] readUShortArray(int size) throws IOException {
        return OtfReadCommon.readUShortArray(rf, size);
    }

    protected void readCoverages(int[] locations, List<HashSet<Integer>> coverage) throws IOException {
        OtfReadCommon.readCoverages(rf, locations, coverage);
    }

	protected final List<Integer> readCoverageFormat(int coverageLocation)
			throws IOException {
        return OtfReadCommon.readCoverageFormat(rf, coverageLocation);
	}

    protected TagAndLocation[] readTagAndLocations(int baseLocation) throws IOException {
        int count = rf.readUnsignedShort();
        TagAndLocation[] tagslLocs = new TagAndLocation[count];
        for (int k = 0; k < count; ++k) {
            TagAndLocation tl = new TagAndLocation();
            tl.tag = rf.readString(4, "utf-8");
            tl.location = rf.readUnsignedShort() + baseLocation;
            tagslLocs[k] = tl;
        }
        return tagslLocs;
    }
    
    protected GposValueRecord ReadGposValueRecord(int mask) throws IOException {
        GposValueRecord vr = new GposValueRecord();
        if ((mask & 0x0001) != 0) {
            vr.XPlacement = rf.readShort();
        }
        if ((mask & 0x0002) != 0) {
            vr.YPlacement = rf.readShort();
        }
        if ((mask & 0x0004) != 0) {
            vr.XAdvance = rf.readShort();
        }
        if ((mask & 0x0008) != 0) {
            vr.YAdvance = rf.readShort();
        }
        if ((mask & 0x0010) != 0) {
            rf.skip(2);
        }
        if ((mask & 0x0020) != 0) {
            rf.skip(2);
        }
        if ((mask & 0x0020) != 0) {
            rf.skip(2);
        }
        if ((mask & 0x0040) != 0) {
            rf.skip(2);
        }
        if ((mask & 0x0080) != 0) {
            rf.skip(2);
        }
        return vr;
    }
}
