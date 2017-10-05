package com.mapbox.services.android.navigation.v5.utils.abbreviation;

import android.util.SparseArray;

final class AbbreviationArray extends SparseArray<AbbreviationArray.Abbreviation> {

  AbbreviationArray() {
    put(size(), new Abbreviation("north", "N"));
    put(size(), new Abbreviation("south", "S"));
    put(size(), new Abbreviation("east", "E"));
    put(size(), new Abbreviation("west", "W"));
    put(size(), new Abbreviation("northwest", "NW"));
    put(size(), new Abbreviation("southwest", "SW"));
    put(size(), new Abbreviation("northeast", "NE"));
    put(size(), new Abbreviation("southeast", "SE"));
    put(size(), new Abbreviation("street", "St"));
    put(size(), new Abbreviation("road", "Rd"));
    put(size(), new Abbreviation("center", "Ctr"));
    put(size(), new Abbreviation("national", "Nat’l"));
    put(size(), new Abbreviation("mount", "Mt"));
    put(size(), new Abbreviation("mountain", "Mtn"));
    put(size(), new Abbreviation("crossing", "Xing"));
    put(size(), new Abbreviation("downtown", "Dtwn"));
    put(size(), new Abbreviation("international", "Int’l"));
    put(size(), new Abbreviation("park", "Pk"));
    put(size(), new Abbreviation("saints", "SS"));
    put(size(), new Abbreviation("heights", "Hts"));
    put(size(), new Abbreviation("route", "Rte"));
    put(size(), new Abbreviation("saint", "St"));
    put(size(), new Abbreviation("fort", "Ft"));
    put(size(), new Abbreviation("market", "Mkt"));
    put(size(), new Abbreviation("centre", "Ctr"));
    put(size(), new Abbreviation("william", "Wm"));
    put(size(), new Abbreviation("school", "Sch"));
    put(size(), new Abbreviation("senior", "Sr"));
    put(size(), new Abbreviation("river", "Riv"));
    put(size(), new Abbreviation("sister", "Sr"));
    put(size(), new Abbreviation("village", "Vil"));
    put(size(), new Abbreviation("station", "Sta"));
    put(size(), new Abbreviation("apartments", "apts"));
    put(size(), new Abbreviation("university", "Univ"));
    put(size(), new Abbreviation("township", "Twp"));
    put(size(), new Abbreviation("lake", "Lk"));
    put(size(), new Abbreviation("junior", "Jr"));
    put(size(), new Abbreviation("father", "Fr"));
    put(size(), new Abbreviation("memorial", "Mem"));
    put(size(), new Abbreviation("junction", "Jct"));
    put(size(), new Abbreviation("court", "Ct"));
    put(size(), new Abbreviation("bypass", "Byp"));
    put(size(), new Abbreviation("drive", "Dr"));
    put(size(), new Abbreviation("motorway", "Mwy"));
    put(size(), new Abbreviation("bridge", "Br"));
    put(size(), new Abbreviation("place", "Pl"));
    put(size(), new Abbreviation("crescent", "Cres"));
    put(size(), new Abbreviation("parkway", "Pky"));
    put(size(), new Abbreviation("lane", "Ln"));
    put(size(), new Abbreviation("avenue", "Ave"));
    put(size(), new Abbreviation("expressway", "Expy"));
    put(size(), new Abbreviation("highway", "Hwy"));
    put(size(), new Abbreviation("square", "Sq"));
    put(size(), new Abbreviation("walkway", "Wky"));
    put(size(), new Abbreviation("pike", "Pk"));
    put(size(), new Abbreviation("freeway", "Fwy"));
    put(size(), new Abbreviation("footway", "Ftwy"));
    put(size(), new Abbreviation("terrace", "Ter"));
    put(size(), new Abbreviation("boulevard", "Blvd"));
    put(size(), new Abbreviation("cove", "Cv"));
    put(size(), new Abbreviation("turnpike", "Tpk"));
    put(size(), new Abbreviation("road", "Rd"));
    put(size(), new Abbreviation("walk", "Wk"));
    put(size(), new Abbreviation("plaza", "Plz"));
    put(size(), new Abbreviation("circle", "Cir"));
    put(size(), new Abbreviation("alley", "Aly"));
    put(size(), new Abbreviation("point", "Pt"));
  }

  static class Abbreviation {
    String string;
    String abbreviatedString;

    Abbreviation(String string, String abbreviatedString) {
      this.string = string;
      this.abbreviatedString = abbreviatedString;
    }
  }
}