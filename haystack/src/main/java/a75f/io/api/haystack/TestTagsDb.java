package a75f.io.api.haystack;

import org.apache.commons.lang3.StringUtils;
import org.projecthaystack.HBool;
import org.projecthaystack.HDateTime;
import org.projecthaystack.HDateTimeRange;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HHisItem;
import org.projecthaystack.HMarker;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;
import org.projecthaystack.HStr;
import org.projecthaystack.HUri;
import org.projecthaystack.HVal;
import org.projecthaystack.HWatch;
import org.projecthaystack.server.HOp;
import org.projecthaystack.server.HStdOps;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import a75f.io.constants.CcuFieldConstants;
import a75f.io.logger.CcuLog;
import io.objectbox.query.QueryBuilder;

/**
 * Test Database without persistence for unit/integration testing of haystack.
 */

public class TestTagsDb extends CCUTagsDb {

    public String addSite(Site s) {
        return addSiteWithId(s, UUID.randomUUID().toString());
    }

    public String addSiteWithId(Site s, String id) {
        HDictBuilder site = new HDictBuilder()
                .add("id", HRef.make(id))
                .add("dis", s.getDisplayName())
                .add("site", HMarker.VAL)
                .add("geoCity", s.getGeoCity())
                .add("geoState", s.getGeoState())
                .add("geoCountry", s.getGeoCountry())
                .add("geoPostalCode", s.getGeoPostalCode())
                .add("geoAddr", "" + s.getGeoAddress())
                .add("tz", s.getTz())
                .add("organization", s.getOrganization())
                .add(CcuFieldConstants.FACILITY_MANAGER_EMAIL, s.getFcManagerEmail())
                .add(CcuFieldConstants.INSTALLER_EMAIL, s.getInstallerEmail())
                .add("area", HNum.make(s.getArea(), "ft\u00B2"));
        if(s.getCreatedDateTime() != null){
            site.add("createdDateTime", s.getCreatedDateTime());
        }
        if(s.getLastModifiedDateTime() != null){
            site.add("lastModifiedDateTime", s.getLastModifiedDateTime());
        }
        if(s.getLastModifiedBy() != null){
            site.add("lastModifiedBy", s.getLastModifiedBy());
        }

        for (String m : s.getMarkers()) {
            site.add(m);
        }

        HRef ref = (HRef) site.get("id");
        tagsMap.put(ref.toVal(), site.toDict());
        return ref.toVal();
    }

    public void updateSite(Site s, String i) {
        HDictBuilder site = new HDictBuilder()
                .add("id", HRef.copy(i))
                .add("dis", s.getDisplayName())
                .add("site", HMarker.VAL)
                .add("geoCity", s.getGeoCity())
                .add("geoState", s.getGeoState())
                .add("geoCountry", s.getGeoCountry())
                .add("geoPostalCode", s.getGeoPostalCode())
                .add("geoAddr", "" + s.getGeoAddress())
                .add("tz", s.getTz())
                .add("organization", s.getOrganization())
                .add(CcuFieldConstants.FACILITY_MANAGER_EMAIL, s.getFcManagerEmail())
                .add(CcuFieldConstants.INSTALLER_EMAIL, s.getInstallerEmail())
                .add("area", HNum.make(s.getArea(), "ft\u00B2"));

        String weatherRef = s.getWeatherRef();
        if (StringUtils.isNotEmpty(weatherRef)) {
            site.add("weatherRef", weatherRef);
        }
        if(s.getCreatedDateTime() != null){
            site.add("createdDateTime", s.getCreatedDateTime());
        }
        if(s.getLastModifiedDateTime() != null){
            site.add("lastModifiedDateTime", s.getLastModifiedDateTime());
        }
        if(s.getLastModifiedBy() != null){
            site.add("lastModifiedBy", s.getLastModifiedBy());
        }

        for (String m : s.getMarkers()) {
            site.add(m);
        }

        HRef id = (HRef) site.get("id");
        tagsMap.put(id.toVal(), site.toDict());
    }

    public void log() {
        int i = 0;
        for (Iterator it = iterator(); it.hasNext(); ) {
            i++;
            HDict rec = (HDict) it.next();
        }
    }



    public String addEquip(Equip q) {
        return addEquipWithId(q, UUID.randomUUID().toString());
    }

    public String addEquipWithId(Equip q, String id) {
        HDictBuilder equip = new HDictBuilder()
                .add("id",      HRef.make(id))
                .add("dis",     q.getDisplayName())
                .add("equip",     HMarker.VAL)
                .add("siteRef", q.getSiteRef())
                .add("roomRef",  q.getRoomRef() != null ? q.getRoomRef() : "SYSTEM")
                .add("floorRef", q.getFloorRef() != null ? q.getFloorRef() : "SYSTEM")
                .add("profile", q.getProfile())
                .add("priorityLevel", q.getPriority())
                .add("tz",q.getTz())
                .add("group",q.getGroup());
        if (q.getAhuRef() != null) {
            equip.add("ahuRef",q.getAhuRef());
        }
        if (q.getCcuRef() != null) {
            equip.add("ccuRef", q.getCcuRef());
        }
        if(q.getGatewayRef() != null){
            equip.add("gatewayRef",q.getGatewayRef());
        }
        if(q.getVendor() != null){
            equip.add("vendor",q.getVendor());
        }
        if(q.getModel() != null){
            equip.add("model",q.getModel());
        }
        if(q.getCreatedDateTime() != null){
            equip.add("createdDateTime", q.getCreatedDateTime());
        }
        if(q.getLastModifiedDateTime() != null){
            equip.add("lastModifiedDateTime", q.getLastModifiedDateTime());
        }
        if(q.getLastModifiedBy() != null){
            equip.add("lastModifiedBy", q.getLastModifiedBy());
        }
        if (q.getDomainName() != null) {
            equip.add("domainName", q.getDomainName());
        }
        if(q.getEquipRef() != null){
            equip.add("equipRef",q.getEquipRef());
        }

        if(q.getEquipType() != null){
            equip.add("equipType", q.getEquipType());
        }

        if (q.getCell() != null) {
            equip.add("cell", q.getCell());
        }
        if (q.getCapacity() != null) {
            equip.add("capacity", q.getCapacity());
        }

        for (String m : q.getMarkers()) {
            equip.add(m);
        }

        q.getTags().entrySet().forEach( entry -> equip.add(entry.getKey(), entry.getValue()));

        HRef ref = (HRef) equip.get("id");
        tagsMap.put(ref.toVal(), equip.toDict());
        return ref.toCode();
    }

    public void updateEquip(Equip q, String i) {
        HDictBuilder equip = new HDictBuilder()
                .add("id",      HRef.copy(i))
                .add("dis",     q.getDisplayName())
                .add("equip",     HMarker.VAL)
                .add("siteRef", q.getSiteRef())
                .add("roomRef",  q.getRoomRef())
                .add("floorRef", q.getFloorRef())
                .add("profile", q.getProfile())
                .add("priorityLevel", q.getPriority())
                .add("tz",q.getTz())
                .add("group",q.getGroup());

        if (q.getAhuRef() != null) {
            equip.add("ahuRef",q.getAhuRef());
        }
        if (q.getCcuRef() != null) {
            equip.add("ccuRef", q.getCcuRef());
        }
        if(q.getCreatedDateTime() != null){
            equip.add("createdDateTime", q.getCreatedDateTime());
        }
        if(q.getLastModifiedDateTime() != null){
            equip.add("lastModifiedDateTime", q.getLastModifiedDateTime());
        }
        if(q.getLastModifiedBy() != null){
            equip.add("lastModifiedBy", q.getLastModifiedBy());
        }
        if(q.getGatewayRef() != null){
            equip.add("gatewayRef",q.getGatewayRef());
        }
        if(q.getVendor() != null){
            equip.add("vendor",q.getVendor());
        }
        if(q.getModel() != null){
            equip.add("model",q.getModel());
        }
        if (q.getDomainName() != null) {
            equip.add("domainName", q.getDomainName());
        }
        if(q.getEquipRef() != null){
            equip.add("equipRef", q.getEquipRef());
        }
        if(q.getEquipType() != null){
            equip.add("equipType", q.getEquipType());
        }
        if(q.getPipeRef() != null){
            equip.add(Tags.PIPEREF, q.getPipeRef());
        }
        if (q.getCell() != null) {
            equip.add("cell", q.getCell());
        }
        if (q.getCapacity() != null) {
            equip.add("capacity", q.getCapacity());
        }
        for (String m : q.getMarkers()) {
            equip.add(m);
        }
        q.getTags().entrySet().forEach( entry -> equip.add(entry.getKey(), entry.getValue()));

        HRef id = (HRef) equip.get("id");
        tagsMap.put(id.toVal(), equip.toDict());
    }


    public HDict getEquip(String dis) {
        return (HDict) tagsMap.get(dis);
    }

    public String addPoint(Point p) {
        return addPointWithId(p, UUID.randomUUID().toString());
    }

    public String addPointWithId(Point p, String id) {
        HDictBuilder b = new HDictBuilder()
                .add("id", HRef.make(id))
                .add("dis", p.getDisplayName())
                .add("point", HMarker.VAL)
                .add("siteRef", p.getSiteRef())
                .add("equipRef", p.getEquipRef())
                .add("roomRef", p.getRoomRef() != null ? p.getRoomRef() : "SYSTEM")
                .add("floorRef", p.getFloorRef() != null ? p.getFloorRef() : "SYSTEM")
                .add("group", p.getGroup())
                .add("kind", p.getKind() == null ? "Number" : p.getKind())
                .add("tz", p.getTz());
        if (p.getUnit() != null) b.add("unit", p.getUnit());
        if (p.getEnums() != null) b.add("enum", p.getEnums());
        if (p.getMinVal() != null) b.add("minVal",Double.parseDouble(p.getMinVal()));
        if (p.getMaxVal() != null) b.add("maxVal",Double.parseDouble(p.getMaxVal()));
        if (p.getCell() != null) b.add("cell", p.getCell());
        if (p.getIncrementVal() != null) b.add("incrementVal",Double.parseDouble(p.getIncrementVal()));
        if (p.getTunerGroup() != null) b.add("tunerGroup",p.getTunerGroup());
        if (p.getHisInterpolate() != null) b.add("hisInterpolate",p.getHisInterpolate());
        if (p.getShortDis() != null) b.add("shortDis",p.getShortDis());
        if(p.getCcuRef() != null){
            b.add("ccuRef", p.getCcuRef());
        }
        if(p.getCreatedDateTime() != null){
            b.add("createdDateTime", p.getCreatedDateTime());
        }
        if(p.getLastModifiedDateTime() != null){
            b.add("lastModifiedDateTime", p.getLastModifiedDateTime());
        }
        if(p.getLastModifiedBy() != null){
            b.add("lastModifiedBy", p.getLastModifiedBy());
        }
        if (p.getDomainName() != null) {
            b.add("domainName", p.getDomainName());
        }

        for (String m : p.getMarkers()) {
            b.add(m);
        }
        p.getTags().entrySet().forEach( entry -> b.add(entry.getKey(), entry.getValue()));
        HRef ref = (HRef) b.get("id");
        tagsMap.put(ref.toVal(), b.toDict());
        return ref.toCode();
    }

    public void updatePoint(Point p, String i) {
        HDictBuilder b = new HDictBuilder()
                .add("id", HRef.copy(i))
                .add("dis", p.getDisplayName())
                .add("point", HMarker.VAL)
                .add("siteRef", p.getSiteRef())
                .add("equipRef", p.getEquipRef())
                .add("roomRef", p.getRoomRef() != null ? p.getRoomRef() : "SYSTEM")
                .add("floorRef", p.getFloorRef() != null ? p.getFloorRef() : "SYSTEM")
                .add("group", p.getGroup())
                .add("kind", p.getKind() == null ? "Number" : p.getKind())
                .add("tz", p.getTz());
        if (p.getUnit() != null) b.add("unit", p.getUnit());
        if (p.getEnums() != null) b.add("enum", p.getEnums());
        if (p.getMinVal() != null) b.add("minVal",Double.parseDouble(p.getMinVal()));
        if (p.getMaxVal() != null) b.add("maxVal",Double.parseDouble(p.getMaxVal()));
        if (p.getIncrementVal() != null) b.add("incrementVal",Double.parseDouble(p.getIncrementVal()));
        if (p.getTunerGroup() != null) b.add("tunerGroup",p.getTunerGroup());
        if (p.getHisInterpolate() != null) b.add("hisInterpolate",p.getHisInterpolate());
        if (p.getShortDis() != null) b.add("shortDis",p.getShortDis());
        if(p.getCcuRef() != null){
            b.add("ccuRef", p.getCcuRef());
        }
        if(p.getCreatedDateTime() != null){
            b.add("createdDateTime", p.getCreatedDateTime());
        }
        if(p.getLastModifiedDateTime() != null){
            b.add("lastModifiedDateTime", p.getLastModifiedDateTime());
        }
        if(p.getLastModifiedBy() != null){
            b.add("lastModifiedBy", p.getLastModifiedBy());
        }
        if (p.getDomainName() != null) {
            b.add("domainName", p.getDomainName());
        }
        for (String m : p.getMarkers()) {
            b.add(m);
        }
        p.getTags().entrySet().forEach( entry -> b.add(entry.getKey(), entry.getValue()));
        HRef id = (HRef) b.get("id");
        tagsMap.put(id.toVal(), b.toDict());
    }

    public String addPoint(RawPoint p) {
        return addPointWithId(p, UUID.randomUUID().toString());
    }

    public String addPointWithId(RawPoint p, String id) {
        HDictBuilder b = new HDictBuilder()
                .add("id", HRef.make(id))
                .add("dis", p.getDisplayName())
                .add("point", HMarker.VAL)
                .add("physical", HMarker.VAL)
                .add("deviceRef", p.getDeviceRef())
                .add("siteRef", p.getSiteRef())
                .add("pointRef", p.getPointRef())
                .add("port", p.getPort())
                .add("analogType", p.getType())
                .add("kind", p.getKind() == null ? "Number" : p.getKind())
                .add("portEnabled",p.getEnabled() ? HBool.TRUE : HBool.FALSE)
                .add("tz", p.getTz());
        if (p.getUnit() != null) b.add("unit", p.getUnit());
        if (p.getShortDis() != null) b.add("shortDis",p.getShortDis());
        if (p.getMinVal() != null) b.add("minVal",Double.parseDouble(p.getMinVal()));
        if (p.getMaxVal() != null) b.add("maxVal",Double.parseDouble(p.getMaxVal()));

        if (p.getRegisterAddress() != null) b.add("registerAddress", p.getRegisterAddress());
        if (p.getRegisterNumber() != null) b.add("registerNumber", p.getRegisterNumber());
        if (p.getStartBit() != null) b.add("startBit", p.getStartBit());
        if (p.getEndBit() != null) b.add("endBit", p.getEndBit());
        if (p.getRegisterType() != null) b.add("registerType", p.getRegisterType());
        if (p.getParameterId() != null) b.add("parameterId", p.getParameterId());
        if(p.getCcuRef() != null){
            b.add("ccuRef", p.getCcuRef());
        }
        if(p.getCreatedDateTime() != null){
            b.add("createdDateTime", p.getCreatedDateTime());
        }
        if(p.getLastModifiedDateTime() != null){
            b.add("lastModifiedDateTime", p.getLastModifiedDateTime());
        }
        if(p.getLastModifiedBy() != null){
            b.add("lastModifiedBy", p.getLastModifiedBy());
        }
        if (p.getDomainName() != null) {
            b.add("domainName", p.getDomainName());
        }
        for (String m : p.getMarkers()) {
            b.add(m);
        }
        p.getTags().entrySet().forEach( entry -> b.add(entry.getKey(), entry.getValue()));
        HRef ref = (HRef) b.get("id");
        tagsMap.put(ref.toVal(), b.toDict());
        return ref.toCode();
    }

    public void updatePoint(RawPoint p, String i) {
        HDictBuilder b = new HDictBuilder()
                .add("id", HRef.copy(i))
                .add("dis", p.getDisplayName())
                .add("point", HMarker.VAL)
                .add("physical", HMarker.VAL)
                .add("deviceRef", p.getDeviceRef())
                .add("siteRef", p.getSiteRef())
                .add("pointRef", p.getPointRef())
                .add("port", p.getPort())
                .add("analogType", p.getType())
                .add("kind", p.getKind() == null ? "Number" : p.getKind())
                .add("portEnabled",p.getEnabled() ? HBool.TRUE : HBool.FALSE)
                .add("tz", p.getTz());
        if (p.getUnit() != null) b.add("unit", p.getUnit());
        if (p.getShortDis() != null) b.add("shortDis",p.getShortDis());
        if (p.getMinVal() != null) b.add("minVal",Double.parseDouble(p.getMinVal()));
        if (p.getMaxVal() != null) b.add("maxVal",Double.parseDouble(p.getMaxVal()));

        if (p.getRegisterAddress() != null) b.add("registerAddress", p.getRegisterAddress());
        if (p.getRegisterNumber() != null) b.add("registerNumber", p.getRegisterNumber());
        if (p.getStartBit() != null) b.add("startBit", p.getStartBit());
        if (p.getEndBit() != null) b.add("endBit", p.getEndBit());
        if (p.getRegisterType() != null) b.add("registerType", p.getRegisterType());
        if (p.getParameterId() != null) b.add("parameterId", p.getParameterId());
        if(p.getCcuRef() != null){
            b.add("ccuRef", p.getCcuRef());
        }
        if(p.getCreatedDateTime() != null){
            b.add("createdDateTime", p.getCreatedDateTime());
        }
        if(p.getLastModifiedDateTime() != null){
            b.add("lastModifiedDateTime", p.getLastModifiedDateTime());
        }
        if(p.getLastModifiedBy() != null){
            b.add("lastModifiedBy", p.getLastModifiedBy());
        }
        if (p.getDomainName() != null) {
            b.add("domainName", p.getDomainName());
        }
        for (String m : p.getMarkers()) {
            b.add(m);
        }
        p.getTags().entrySet().forEach( entry -> b.add(entry.getKey(), entry.getValue()));
        HRef id = (HRef) b.get("id");
        tagsMap.put(id.toVal(), b.toDict());
    }

    public String addPoint(SettingPoint p) {
        return addPointWithId(p, UUID.randomUUID().toString());
    }

    public String addPointWithId(SettingPoint p, String id) {
        HDictBuilder b = new HDictBuilder()
                .add("id", HRef.make(id))
                .add("dis", p.getDisplayName())
                .add("point", HMarker.VAL)
                .add("setting", HMarker.VAL)
                .add("deviceRef", p.getDeviceRef())
                .add("siteRef", p.getSiteRef())
                .add("val", p.getVal())
                .add("kind", p.getKind() == null ? "Str" : p.getKind());

        if (p.getUnit() != null) b.add("unit", p.getUnit());
        if(p.getCcuRef() != null){
            b.add("ccuRef", p.getCcuRef());
        }
        if(p.getCreatedDateTime() != null){
            b.add("createdDateTime", p.getCreatedDateTime());
        }
        if(p.getLastModifiedDateTime() != null){
            b.add("lastModifiedDateTime", p.getLastModifiedDateTime());
        }
        if(p.getLastModifiedBy() != null){
            b.add("lastModifiedBy", p.getLastModifiedBy());
        }
        if (p.getDomainName() != null) {
            b.add("domainName", p.getDomainName());
        }
        for (String m : p.getMarkers()) {
            b.add(m);
        }
        HRef ref = (HRef) b.get("id");
        tagsMap.put(ref.toVal(), b.toDict());
        return ref.toCode();
    }

    public String addDevice(Device d) {
        return addDeviceWithId(d, UUID.randomUUID().toString());
    }

    public String addDeviceWithId(Device d, String id) {
        HDictBuilder b = new HDictBuilder()
                .add("id", HRef.make(id))
                .add("dis", d.getDisplayName())
                .add("device", HMarker.VAL)
                .add("addr", d.getAddr())
                .add("siteRef", d.getSiteRef())
                .add("equipRef", d.getEquipRef())
                .add("roomRef", d.getRoomRef() != null ? d.getRoomRef() : "SYSTEM")
                .add("floorRef", d.getFloorRef() != null ? d.getFloorRef() : "SYSTEM");

        if (d.getCcuRef() != null) {
            b.add("ccuRef", d.getCcuRef());
        }
        if(d.getCreatedDateTime() != null){
            b.add("createdDateTime", d.getCreatedDateTime());
        }
        if(d.getLastModifiedDateTime() != null){
            b.add("lastModifiedDateTime", d.getLastModifiedDateTime());
        }
        if(d.getLastModifiedBy() != null){
            b.add("lastModifiedBy", d.getLastModifiedBy());
        }
        if (d.getDomainName() != null) {
            b.add("domainName", d.getDomainName());
        }
        for (String m : d.getMarkers()) {
            b.add(m);
        }
        d.getTags().entrySet().forEach( entry -> b.add(entry.getKey(), entry.getValue()));
        HRef ref = (HRef) b.get("id");
        tagsMap.put(ref.toVal(), b.toDict());
        return ref.toCode();
    }

    public void updateDevice(Device d, String i) {
        HDictBuilder b = new HDictBuilder()
                .add("id", HRef.copy(i))
                .add("dis", d.getDisplayName())
                .add("device", HMarker.VAL)
                .add("his", HMarker.VAL)
                .add("addr", d.getAddr())
                .add("siteRef", d.getSiteRef())
                .add("equipRef", d.getEquipRef())
                .add("roomRef", d.getRoomRef())
                .add("floorRef", d.getFloorRef());

        if (d.getCcuRef() != null) {
            b.add("ccuRef", d.getCcuRef());
        }
        if(d.getCreatedDateTime() != null){
            b.add("createdDateTime", d.getCreatedDateTime());
        }
        if(d.getLastModifiedDateTime() != null){
            b.add("lastModifiedDateTime", d.getLastModifiedDateTime());
        }
        if(d.getLastModifiedBy() != null){
            b.add("lastModifiedBy", d.getLastModifiedBy());
        }
        if (d.getDomainName() != null) {
            b.add("domainName", d.getDomainName());
        }
        for (String m : d.getMarkers()) {
            b.add(m);
        }
        d.getTags().entrySet().forEach( entry -> b.add(entry.getKey(), entry.getValue()));
        HRef id = (HRef) b.get("id");
        tagsMap.put(id.toVal(), b.toDict());
    }

    public String addFloor(Floor f) {
        return addFloorWithId(f, UUID.randomUUID().toString());
    }

    public String addFloorWithId(Floor f, String id) {
        HDictBuilder b = new HDictBuilder()
                .add("id", HRef.make(id))
                .add("dis", f.getDisplayName())
                .add("floor", HMarker.VAL)
                .add("siteRef", f.getSiteRef())
                .add("orientation", 0.0)
                .add("floorNum", 0.0);
        if(f.getCreatedDateTime() != null){
            b.add("createdDateTime", f.getCreatedDateTime());
        }
        if(f.getLastModifiedDateTime() != null){
            b.add("lastModifiedDateTime", f.getLastModifiedDateTime());
        }
        if(f.getLastModifiedBy() != null){
            b.add("lastModifiedBy", f.getLastModifiedBy());
        }

        for (String m : f.getMarkers()) {
            b.add(m);
        }
        HRef ref = (HRef) b.get("id");
        tagsMap.put(ref.toVal(), b.toDict());
        return ref.toCode();
    }

    public void updateFloor(Floor f, String i) {
        HDictBuilder b = new HDictBuilder()
                .add("id", HRef.copy(i))
                .add("dis", f.getDisplayName())
                .add("floor", HMarker.VAL)
                .add("siteRef", f.getSiteRef())
                .add("orientation", f.getOrientation())
                .add("floorNum", f.getFloorNum());
        if(f.getCreatedDateTime() != null){
            b.add("createdDateTime", f.getCreatedDateTime());
        }
        if(f.getLastModifiedDateTime() != null){
            b.add("lastModifiedDateTime", f.getLastModifiedDateTime());
        }
        if(f.getLastModifiedBy() != null){
            b.add("lastModifiedBy", f.getLastModifiedBy());
        }

        for (String m : f.getMarkers()) {
            b.add(m);
        }
        HRef id = (HRef) b.get("id");
        tagsMap.put(id.toVal(), b.toDict());
    }

    public String addZone(Zone z) {
        return addZoneWithId(z, UUID.randomUUID().toString());
    }

    public String addZoneWithId(Zone z, String id) {
        HDictBuilder b = new HDictBuilder()
                .add("id", HRef.make(id))
                .add("dis", z.getDisplayName())
                .add("room", HMarker.VAL)
                .add("siteRef", z.getSiteRef())
                .add("scheduleRef", z.getScheduleRef())
                .add("floorRef", z.getFloorRef())
                .add("ccuRef", z.getCcuRef());

        if(z.getCreatedDateTime() != null){
            b.add("createdDateTime", z.getCreatedDateTime());
        }
        if(z.getLastModifiedDateTime() != null){
            b.add("lastModifiedDateTime", z.getLastModifiedDateTime());
        }
        if(z.getLastModifiedBy() != null){
            b.add("lastModifiedBy", z.getLastModifiedBy());
        }

        for (String m : z.getMarkers()) {
            b.add(m);
        }
        HRef ref = (HRef) b.get("id");
        tagsMap.put(ref.toVal(), b.toDict());
        return ref.toCode();
    }

    public void updateZone(Zone z, String i) {
        HDictBuilder b = new HDictBuilder()
                .add("id", HRef.copy(i))
                .add("dis", z.getDisplayName())
                .add("room", HMarker.VAL)
                .add("siteRef", z.getSiteRef())
                .add("scheduleRef", z.getScheduleRef())
                .add("floorRef", z.getFloorRef())
                .add("ccuRef", z.getCcuRef());
        if (z.getScheduleRef() != null) {
            b.add("scheduleRef", z.getScheduleRef());
        }
        if(z.getCreatedDateTime() != null){
            b.add("createdDateTime", z.getCreatedDateTime());
        }
        if(z.getLastModifiedDateTime() != null){
            b.add("lastModifiedDateTime", z.getLastModifiedDateTime());
        }
        if(z.getLastModifiedBy() != null){
            b.add("lastModifiedBy", z.getLastModifiedBy());
        }

        for (String m : z.getMarkers()) {
            b.add(m);
        }
        HRef id = (HRef) b.get("id");
        tagsMap.put(id.toVal(), b.toDict());
    }


    //////////////////////////////////////////////////////////////////////////
    // Ops
    //////////////////////////////////////////////////////////////////////////

    public HOp[] ops() {
        return new HOp[]{
                HStdOps.about,
                HStdOps.ops,
                HStdOps.formats,
                HStdOps.read,
                HStdOps.nav,
                HStdOps.pointWrite,
                HStdOps.hisWrite,
                HStdOps.hisRead,
                HStdOps.invokeAction,
        };
    }

    public HDict onAbout() {
        return about;
    }

    private final HDict about = new HDictBuilder()
            .add("serverName", hostName())
            .add("vendorName", "Haystack Java Toolkit")
            .add("vendorUri", HUri.make("http://project-haystack.org/"))
            .add("productName", "Haystack Java Toolkit")
            .add("productVersion", "2.0.0")
            .add("productUri", HUri.make("http://project-haystack.org/"))
            .toDict();

    private static String hostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    //////////////////////////////////////////////////////////////////////////
    // Reads
    //////////////////////////////////////////////////////////////////////////

    protected HDict onReadById(HRef id) {
        return (HDict) tagsMap.get(id.val);
    }

    protected Iterator iterator() {
        return tagsMap.values().iterator();
    }

    //////////////////////////////////////////////////////////////////////////
    // Navigation
    //////////////////////////////////////////////////////////////////////////

    protected HGrid onNav(String navId) {
        // test database navId is record id
        HDict base = null;
        if (navId != null) base = readById(HRef.make(navId));

        // map base record to site, equip, or point
        String filter = "site";
        if (base != null) {
            if (base.has("site")) filter = "equip and siteRef==\"" + base.id().toCode() + "\"";
            else if (base.has("equip"))
                filter = "point and equipRef==\"" + base.id().toCode() + "\"";
            else filter = "navNoChildren";
        }

        // read children of base record
        HGrid grid = readAll(filter);

        // add navId column to results
        HDict[] rows = new HDict[grid.numRows()];
        Iterator it = grid.iterator();
        for (int i = 0; it.hasNext(); ) rows[i++] = (HDict) it.next();
        for (int i = 0; i < rows.length; ++i)
            rows[i] = new HDictBuilder().add(rows[i]).add("navId", rows[i].id().val).toDict();
        return HGridBuilder.dictsToGrid(rows);
    }

    protected HDict onNavReadByUri(HUri uri) {
        return null;
    }

    //////////////////////////////////////////////////////////////////////////
    // Watches
    //////////////////////////////////////////////////////////////////////////

    protected HWatch onWatchOpen(String dis, HNum lease) {
        throw new UnsupportedOperationException();
    }

    protected HWatch[] onWatches() {
        throw new UnsupportedOperationException();
    }

    protected HWatch onWatch(String id) {
        throw new UnsupportedOperationException();
    }

    //////////////////////////////////////////////////////////////////////////
    // Point Write
    //////////////////////////////////////////////////////////////////////////

    protected HGrid onPointWriteArray(HDict rec) {
        CCUTagsDb.WriteArray array = (CCUTagsDb.WriteArray) writeArrays.get(rec.id().toVal());
        if (array == null) array = new CCUTagsDb.WriteArray();

        HGridBuilder b = new HGridBuilder();
        b.addCol("level");
        b.addCol("levelDis");
        b.addCol("val");
        b.addCol("who");
        b.addCol("duration");
        b.addCol("lastModifiedDateTime");

        for (int i = 0; i < 17; ++i) {

            if (array.duration[i] != 0 && array.duration[i] < System.currentTimeMillis()) {
                array.val[i] = null;
                array.who[i] = null;
                array.duration[i] = 0;
            }
            b.addRow(new HVal[]{
                    HNum.make(i + 1),
                    HStr.make("" + (i + 1)),
                    array.val[i],
                    HStr.make(array.who[i]),
                    HNum.make(array.duration[i] >  System.currentTimeMillis() ? array.duration[i] : 0),
                    array.lastModifiedDateTime[i]
            });
        }
        return b.toGrid();
    }

    protected void onPointWrite(HDict rec, int level, HVal val, String who, HNum dur, HDict opts,
                                HDateTime lastModifiedDateTime) {
        CcuLog.d(TAG_CCU_HS,"onPointWrite: " + rec.dis() + "  " + val + " @ " + level + " [" + who + "]"+", duration: "+dur.millis());
        CCUTagsDb.WriteArray array = (CCUTagsDb.WriteArray) writeArrays.get(rec.id().toVal());
        if (array == null) writeArrays.put(rec.id().toVal(), array = new CCUTagsDb.WriteArray());
        array.val[level - 1] = val;
        array.who[level - 1] = who;
        array.duration[level-1] = dur.val > 0 ? System.currentTimeMillis() + dur.millis() : 0;
        array.lastModifiedDateTime[level-1]= lastModifiedDateTime;
    }

    public void deletePointArray(HRef id) {
        writeArrays.remove(id.toVal());
    }

    public void deletePointArrayLevel(HRef id, int level) {
        CCUTagsDb.WriteArray array = (CCUTagsDb.WriteArray) writeArrays.get(id.toVal());
        if (array != null) {
            array.val[level - 1] = null;
            array.who[level - 1] = null;
            array.duration[level-1] = 0;
        } else {
            CcuLog.d(TAG_CCU_HS," Invalid point array delete command "+id);
        }

    }

    public HDict getConfig() {
        if (!tagsMap.containsKey("config")) {
            HDict hDict = new HDictBuilder().add("nosync").add("localconfig").toDict();
            tagsMap.put("config", hDict);
        }
        return tagsMap.get("config");
    }

    public HDict updateConfig(String propertyName, HVal hVal) {
        HDict config = getConfig();
        HDict hDict = new HDictBuilder().add(config).add(propertyName, hVal).toDict();
        tagsMap.put("config", hDict);
        return hDict;
    }

    public void addHDict(String localId, HDict hDict) {
        tagsMap.put(localId, hDict);
    }

    static class WriteArray {
        final HVal[] val = new HVal[17];
        final String[] who = new String[17];
        final long[] duration = new long[17];
        final HDateTime[] lastModifiedDateTime = new HDateTime[17];
    }

    //////////////////////////////////////////////////////////////////////////
    // History
    //////////////////////////////////////////////////////////////////////////

    public HHisItem[] onHisRead(HDict entity, HDateTimeRange range) {
        QueryBuilder<HisItem> hisQuery = hisBox.query();
        hisQuery.equal(HisItem_.rec, entity.get("id").toString())
                .greater(HisItem_.date, range.start.millis())
                .less(HisItem_.date, range.end.millis())
                .order(HisItem_.date);

        List<HisItem> hisList = hisQuery.build().find();

        boolean isBool = ((HStr) entity.get("kind")).val.equals("Bool");
        ArrayList acc = new ArrayList();
        for (HisItem item : hisList) {
            HVal val = isBool ? HBool.make(item.val > 0) : HNum.make(item.val);
            HDict hsItem = HHisItem.make(HDateTime.make(item.getDate().getTime()), val);
            if (item.getDate().getTime() != range.start.millis()) {
                acc.add(hsItem);
            }
        }
        return (HHisItem[]) acc.toArray(new HHisItem[acc.size()]);
    }

    public HHisItem[] onHisRead(HDict entity) {
        QueryBuilder<HisItem> hisQuery = hisBox.query();
        hisQuery.equal(HisItem_.rec, entity.get("id").toString())
                .order(HisItem_.date, QueryBuilder.DESCENDING);

        HisItem item = hisQuery.build().findFirst();

        boolean isBool = ((HStr) entity.get("kind")).val.equals("Bool");
        ArrayList acc = new ArrayList();

        HVal val = isBool ? HBool.make(item.val > 0) : HNum.make(item.val);
        HDict hsItem = HHisItem.make(HDateTime.make(item.getDate().getTime()), val);
        acc.add(hsItem);
        return (HHisItem[]) acc.toArray(new HHisItem[acc.size()]);
    }

    public void onHisWrite(HDict rec, HHisItem[] items) {
        saveHisItemsToCache(rec,items, false);
    }

    public void saveHisItemsToCache (HDict rec, HHisItem[] items, boolean syncItems) {
        for (HHisItem item : items) {
            HisItem hisItem = new HisItem();
            hisItem.setDate(new Date(item.ts.millis()));
            hisItem.setRec(rec.get("id").toString());
            hisItem.setVal(Double.parseDouble(item.val.toString()));
            hisItem.setSyncStatus(syncItems);
            CcuLog.d(TAG_CCU_HS,"Write historized value to local DB for point ID " + rec.get("id").toString() + "; description " + rec.get("dis").toString() + "; value "  + item.val.toString());
            hisBox.put(hisItem);
            HisItemCache.getInstance().add(rec.get("id").toString(), hisItem);
        }
    }

    //////////////////////////////////////////////////////////////////////////
    // Actions
    //////////////////////////////////////////////////////////////////////////

    public HGrid onInvokeAction(HDict rec, String action, HDict args) {
        CcuLog.d(TAG_CCU_HS,"-- invokeAction \"" + rec.dis() + "." + action + "\" " + args);
        return HGrid.EMPTY;
    }

    public List<HisItem> getUnsyncedHisItemsOrderDesc(String pointId) {
        List<HisItem> validHisItems = new ArrayList<>();

        QueryBuilder<HisItem> hisQuery = hisBox.query();
        hisQuery.equal(HisItem_.rec, pointId)
                .equal(HisItem_.syncStatus, false)
                .orderDesc(HisItem_.date);
        List<HisItem> hisItems = hisQuery.build().find();
        // TODO Matt Rudd - This shouldn't be necessary, but I was seeing null items in the collection; need to investigate
        for (HisItem hisItem : hisItems) {
            if  (hisItem != null) {
                validHisItems.add(hisItem);
            }
        }
        //CcuLog.d(TAG_CCU_HS, "Finding unsynced items for point ID " + pointId+" size: "+validHisItems.size());
        return validHisItems;
    }

    public HisItem getLastHisItem(HRef id) {
        //long time = System.currentTimeMillis();
        HisItem retVal = HisItemCache.getInstance().get(id.toString());
        if (retVal == null) {
            retVal = hisBox.query().equal(HisItem_.rec, id.toString())
                    .orderDesc(HisItem_.date).build().findFirst();

            if (retVal != null) {
                HisItemCache.getInstance().add(id.toString(), retVal);
            }
        }
        //CcuLog.d(TAG_CCU_HS, "getLastHisItem "+id+" : timeMS: "+(System.currentTimeMillis() - time));
        return retVal;
    }

    public void putHisItem(String id, Double val) {
        HisItem hisItem = new HisItem();
        hisItem.setDate(new Date(System.currentTimeMillis()));
        hisItem.setRec(id);
        hisItem.setVal(val);
        hisBox.put(hisItem);
        HisItemCache.getInstance().add(id, hisItem);
        CcuLog.d(TAG_CCU_HS, "Write historized value to local DB for point ID " + id + "; value "  + val);
    }

    public void addHisItemToCache(String id) {
        if (id == null || id.isEmpty()) {
            return;
        }
        HisItem retVal = HisItemCache.getInstance().get(id);
        if (retVal != null) {
            return;
        }
        HisItem hisItem = hisBox.query()
                .equal(HisItem_.rec, id)
                .orderDesc(HisItem_.date)
                .build()
                .findFirst();

        if (hisItem != null) {
            HisItemCache.getInstance().add(id, hisItem);
        }
    }

    public void putHisItems(List<HisItem> hisItemList) {
        hisBox.put(hisItemList);
        hisItemList.forEach(entry -> {
            HisItemCache.getInstance().add(entry.getRec(), entry);
        });
    }

    public void updateHisItemSynced(List<HisItem> hisItems) {
        for (HisItem item : hisItems) {
            item.setSyncStatus(true);
            hisBox.put(item);
        }
    }

    public void updateHisItems(List<HisItem> hisItems) {
        for (HisItem item : hisItems) {
            hisBox.put(item);
        }
    }

    public List<HisItem> getHisItemsForMigration(HRef id, int offset, int limit) {

        QueryBuilder<HisItem> hisQuery = hisBox.query();
        hisQuery.equal(HisItem_.rec, id.toString())
                .orderDesc(HisItem_.date);
        return hisQuery.build().find(offset, limit);
    }

    public List<HisItem> getHisItems(HRef id, int offset, int limit) {

        HDict entity = readById(id);

        QueryBuilder<HisItem> hisQuery = hisBox.query();
        hisQuery.equal(HisItem_.rec, entity.get("id").toString())
                .orderDesc(HisItem_.date);
        return hisQuery.build().find(offset, limit);
    }

    //Delete all the hisItem entries older than 24 hrs.
    public void removeExpiredHisItems(HRef id) {
        HDict entity = readById(id);
        QueryBuilder<HisItem> hisQuery = hisBox.query();
        hisQuery.equal(HisItem_.rec, entity.get("id").toString())
                .less(HisItem_.date, System.currentTimeMillis() - 24*60*60*1000)
                .or()
                .equal(HisItem_.syncStatus, true)
                .order(HisItem_.date);
        //Leave 2 hisItem entries to make sure there is atleast one his data and "delta" alert validation
        //can be performed , which requires atleast 2 entries.
        List<HisItem>  hisItems = hisQuery.build().find();
        if (hisItems.size() > 2) {
            hisItems.remove(hisItems.size() - 1);
            hisItems.remove(hisItems.size() - 1);
            hisBox.remove(hisItems);
        }
    }

    public void removeAllHisItems(HRef id) {
        HDict entity = readById(id);

        QueryBuilder<HisItem> hisQuery = hisBox.query();
        hisQuery.equal(HisItem_.rec, entity.get("id").toString())
                .order(HisItem_.date);
        List<HisItem>  hisItems = hisQuery.build().find();

        //Leave one hisItem to make sure his data is not empty if there was no more recent entries
        if (hisItems.size() > 1)
        {
            hisItems.remove(hisItems.size() - 1);
            hisBox.remove(hisItems);
        }
    }

    public void clearHistory(HRef id) {
        HDict entity = readById(id);

        QueryBuilder<HisItem> hisQuery = hisBox.query();
        hisQuery.equal(HisItem_.rec, entity.get("id").toString())
                .order(HisItem_.date);
        List<HisItem>  hisItems = hisQuery.build().find();
        hisBox.remove(hisItems);
    }
}

