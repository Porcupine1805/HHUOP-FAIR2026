package org.hhuop.model;

import java.util.*;

public record HUOPPattern(Itemset itemset, int support, double sumUO, double uo, List<Integer> tids) {
    public HUOPPattern { tids = List.copyOf(tids); }
}
