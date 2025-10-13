package com.blz.summer.port;

public interface LikeCounterPort {

    long incrementAtomic(String slug);

    long get(String slug);

}
