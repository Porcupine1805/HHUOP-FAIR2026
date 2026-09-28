package org.hhuop.sanitize;
import org.hhuop.model.QuantitativeDatabase;
import org.hhuop.mining.HUOPMiningResult;
import java.util.*;
public record SanitizationResult(QuantitativeDatabase database, HUOPMiningResult finalMining,
                                 SanitizationMetrics metrics, List<SanitizationAction> actions) {
    public SanitizationResult { actions = List.copyOf(actions); }
}
