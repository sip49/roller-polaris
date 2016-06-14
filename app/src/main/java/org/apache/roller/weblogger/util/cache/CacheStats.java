package org.apache.roller.weblogger.util.cache;

import java.time.LocalDateTime;

public class CacheStats {

    private LocalDateTime startTime;
    private int hits;
    private int misses;
    private int puts;
    private int removes;
    private double efficiency = 0.0;

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public int getHits() {
        return hits;
    }

    public void setHits(int hits) {
        this.hits = hits;
    }

    public int getMisses() {
        return misses;
    }

    public void setMisses(int misses) {
        this.misses = misses;
    }

    public int getPuts() {
        return puts;
    }

    public void setPuts(int puts) {
        this.puts = puts;
    }

    public int getRemoves() {
        return removes;
    }

    public void setRemoves(int removes) {
        this.removes = removes;
    }

    public double getEfficiency() {
        return efficiency;
    }

    public void setEfficiency(double efficiency) {
        this.efficiency = efficiency;
    }
}
