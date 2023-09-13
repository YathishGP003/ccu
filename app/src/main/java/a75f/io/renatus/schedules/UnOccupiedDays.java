package a75f.io.renatus.schedules;

import org.projecthaystack.HDict;
import org.projecthaystack.HList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import a75f.io.api.haystack.Tags;

public class UnOccupiedDays {

        private int day;
        private int sthh;
        private int stmm;
        private int ethh;
        private int etmm;
        private boolean intersection;

        public UnOccupiedDays(){
        }

        public UnOccupiedDays(int day, int sthh, int stmm, int ethh, int etmm, boolean intersection) {
            this.day = day;
            this.sthh = sthh;
            this.stmm = stmm;
            this.ethh = ethh;
            this.etmm = etmm;
            this.intersection = intersection;
        }

        public int getDay() {
            return day;
        }

        public void setDay(int day) {
            this.day = day;
        }

        public int getSthh() {
            return sthh;
        }

        public void setSthh(int sthh) {
            this.sthh = sthh;
        }

        public int getStmm() {
            return stmm;
        }

        public void setStmm(int stmm) {
            this.stmm = stmm;
        }

        public int getEthh() {
            return ethh;
        }

        public void setEthh(int ethh) {
            this.ethh = ethh;
        }

        public int getEtmm() {
            return etmm;
        }

        public void setEtmm(int etmm) {
            this.etmm = etmm;
        }

        public boolean isIntersection() {
            return intersection;
        }

        public void setIntersection(boolean intersection) {
            this.intersection = intersection;
        }

        public static List<UnOccupiedDays> parse(HList value) {
            List<UnOccupiedDays> days = new ArrayList<>();
            for (int i = 0; i < value.size(); i++) {
                days.add(parseSingleDay((HDict) value.get(i)));
            }
            return days;
        }

        public static UnOccupiedDays parseSingleDay(HDict hDict) {
            UnOccupiedDays days = new UnOccupiedDays();
            days.day = hDict.getInt(Tags.DAY);
            days.ethh = hDict.has(Tags.ETHH) ? hDict.getInt(Tags.ETHH) : -1;
            days.etmm = hDict.has(Tags.ETMM) ? hDict.getInt(Tags.ETMM) : -1;
            days.sthh = hDict.has(Tags.STHH) ? hDict.getInt(Tags.STHH) : -1;
            days.stmm = hDict.has(Tags.STMM) ? hDict.getInt(Tags.STMM) : -1;
            return days;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof UnOccupiedDays)) return false;
            UnOccupiedDays days = (UnOccupiedDays) o;
            return getDay() == days.getDay() && getSthh() == days.getSthh() && getStmm() == days.getStmm() &&
                    getEthh() == days.getEthh() && getEtmm() == days.getEtmm() &&
                    isIntersection() == days.isIntersection();
        }

        @Override
        public int hashCode() {
            return Objects.hash(getDay(), getSthh(), getStmm(), getEthh(), getEtmm(), isIntersection());
        }

        @Override
        public String toString() {
            return "Days{" +
                    "day=" + day +
                    ", sthh=" + sthh +
                    ", stmm=" + stmm +
                    ", ethh=" + ethh +
                    ", etmm=" + etmm +
                    ", intersection=" + intersection +
                    '}';
        }

}
