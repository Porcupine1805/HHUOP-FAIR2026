package org.hhuop.sanitize;

import org.hhuop.model.Itemset;

public record SanitizationAction(
        int step, String algorithm, Itemset target, int tid, String item,
        int beforeQuantity, int afterQuantity, double utilityLoss,
        int predictedMissing, int predictedArtificial, double score) {
    @Override public String toString() {
        return step + "," + algorithm + ",\"" + target + "\"," + tid + "," + item + "," +
                beforeQuantity + "," + afterQuantity + "," + utilityLoss + "," +
                predictedMissing + "," + predictedArtificial + "," + score;
    }
}
