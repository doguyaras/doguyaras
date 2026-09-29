package com.acme.platform.messaging.readmodel;

/** Kaynak basina bir delta projeksiyonu. apply yalniz sira dogrulandiktan sonra cagrilir (ReadModelApplier). */
public interface DeltaProjection<D> {

    void apply(DeltaEvent<D> event);

    void clear();
}
