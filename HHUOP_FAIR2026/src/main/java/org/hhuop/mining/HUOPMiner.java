package org.hhuop.mining;
import org.hhuop.model.QuantitativeDatabase;
public interface HUOPMiner {
    HUOPMiningResult mine(QuantitativeDatabase db, double alpha, double beta);
}
