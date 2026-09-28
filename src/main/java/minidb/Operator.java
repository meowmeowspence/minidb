package minidb;

public interface Operator
        extends AutoCloseable {

    void open();

    Tuple next();

    Schema getOutputSchema();

    @Override
    void close();
}