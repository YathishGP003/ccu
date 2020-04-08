/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2019 75F.io  All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * When signing a commercial license with Infinite Automation Software,
 * the following extension to GPL is made. A special exception to the GPL is
 * included to allow you to distribute a combined work that includes BAcnet4J
 * without being obliged to provide the source code for any proprietary components.
 *
 * See www.75f.io for commercial license options.
 * 
 * @author Suresh Kumar
 */
package com.renovo.bacnet4j.enums;

public enum Month {
    JANUARY(1), FEBRUARY(2), MARCH(3), APRIL(4), MAY(5), JUNE(6), JULY(7), AUGUST(8), SEPTEMBER(9), OCTOBER(10), NOVEMBER(
            11), DECEMBER(12), ODD_MONTHS(13), EVEN_MONTHS(14), UNSPECIFIED(255);

    public byte id;

    Month(int id) {
        this.id = (byte) id;
    }

    public byte getId() {
        return id;
    }

    public static Month valueOf(int id) {
        return valueOf((byte) id);
    }

    public boolean isSpecific() {
        switch (this) {
        case ODD_MONTHS:
        case EVEN_MONTHS:
        case UNSPECIFIED:
            return false;
        default:
            return true;
        }
    }

    public boolean isOdd() {
        switch (this) {
        case JANUARY:
        case MARCH:
        case MAY:
        case JULY:
        case SEPTEMBER:
        case NOVEMBER:
            return true;
        default:
            return false;
        }
    }

    public boolean isEven() {
        switch (this) {
        case FEBRUARY:
        case APRIL:
        case JUNE:
        case AUGUST:
        case OCTOBER:
        case DECEMBER:
            return true;
        default:
            return false;
        }
    }

    public boolean matches(Month that) {
        if (this == Month.UNSPECIFIED)
            return true;
        if (this == Month.ODD_MONTHS)
            return that.isOdd();
        if (this == Month.EVEN_MONTHS)
            return that.isEven();
        return this == that;
    }

    public static Month valueOf(byte id) {
        if (id == JANUARY.id)
            return JANUARY;
        if (id == FEBRUARY.id)
            return FEBRUARY;
        if (id == MARCH.id)
            return MARCH;
        if (id == APRIL.id)
            return APRIL;
        if (id == MAY.id)
            return MAY;
        if (id == JUNE.id)
            return JUNE;
        if (id == JULY.id)
            return JULY;
        if (id == AUGUST.id)
            return AUGUST;
        if (id == SEPTEMBER.id)
            return SEPTEMBER;
        if (id == OCTOBER.id)
            return OCTOBER;
        if (id == NOVEMBER.id)
            return NOVEMBER;
        if (id == DECEMBER.id)
            return DECEMBER;
        if (id == ODD_MONTHS.id)
            return ODD_MONTHS;
        if (id == EVEN_MONTHS.id)
            return EVEN_MONTHS;
        return UNSPECIFIED;
    }

    public static int getIDof(Month id) {
        if (id == JANUARY)
            return JANUARY.id;
        if (id == FEBRUARY)
            return FEBRUARY.id;
        if (id == MARCH)
            return MARCH.id;
        if (id == APRIL)
            return APRIL.id;
        if (id == MAY)
            return MAY.id;
        if (id == JUNE)
            return JUNE.id;
        if (id == JULY)
            return JULY.id;
        if (id == AUGUST)
            return AUGUST.id;
        if (id == SEPTEMBER)
            return SEPTEMBER.id;
        if (id == OCTOBER)
            return OCTOBER.id;
        if (id == NOVEMBER)
            return NOVEMBER.id;
        if (id == DECEMBER)
            return DECEMBER.id;
        if (id == ODD_MONTHS)
            return ODD_MONTHS.id;
        if (id == EVEN_MONTHS)
            return EVEN_MONTHS.id;
        return UNSPECIFIED.id;
    }
}
