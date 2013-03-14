package uk.ac.standrews.cs.shabdiz.examples.echo;

import uk.ac.standrews.cs.jetson.exception.JsonRpcException;

public interface EchoService {

    String echo(String message) throws JsonRpcException;

    void shutdown() throws JsonRpcException;
}
