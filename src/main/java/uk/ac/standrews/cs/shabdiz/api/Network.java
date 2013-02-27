package uk.ac.standrews.cs.shabdiz.api;

import java.util.Set;

public interface Network<Member> extends Set<Member> {

    Member deploy(Host host);

    void kill(Member member);

    void shutdown();

    //    void add(Host host);
    //    void killAny(Host host);
    //    void deployAll();
    //    void killAll();
}
