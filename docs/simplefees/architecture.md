# Simple Fees Architecture

This is the architecture for simple fees aka. *Fees 2.0*.

The Fees schedule is defined by the AbstractFeesSchedule interface with two implementations, one to load the schedule
from JSON and the other one for unit testing using in-memory fees.

```mermaid
---
title: Fees Schedule
---
classDiagram
    AbstractFeesSchedule <|-- JsonFeesSchedule : implements
    AbstractFeesSchedule <|-- MockFeesSchedule : implements
    
    note for AbstractFeesSchedule "This is a note for a class"
    link AbstractFeesSchedule "https://github.com/joshmarinacci/hiero-consensus-node/blob/simple-fees-research/hedera-node/hedera-app-spi/src/main/java/com/hedera/node/app/hapi/fees/AbstractFeesSchedule.java" "source"
    link JsonFeesSchedule "https://github.com/joshmarinacci/hiero-consensus-node/blob/simple-fees-research/hedera-node/hedera-app-spi/src/main/java/com/hedera/node/app/hapi/fees/JsonFeesSchedule.java" "source"
    link MockFeesSchedule "https://github.com/joshmarinacci/hiero-consensus-node/blob/simple-fees-research/hedera-node/hedera-app-spi/src/main/java/com/hedera/node/app/hapi/fees/MockFeesSchedule.java" "source"

    class AbstractFeesSchedule {
         <<interface>>
        + List getDefinedExtrasNames()
        + List~String~ getDefinedExtraNames()
        + long getExtrasFee(String name)
        + long getNodeBaseFee()
        + List~String~ getNodeExtraNames()
        + long getNodeExtraIncludedCount(String name)
        + long getNetworkMultiplier()
        + List~String~ getServiceNames()
        + long getServiceBaseFee(String method)
        + List~String~ getServiceExtras(String method)
        + long getServiceExtraIncludedCount(String method, String name)
    }
    
    class JsonFeesSchedule {
        + static JsonFeesSchedule fromJson();
    }
    
    class MockFeesSchedule {
        + setExtrasFee(String name, long value)
        + setExtrasFee(Extras name, long value)
        + setNodeBaseFee(long value)
        + setNodeExtraIncludedCount(String signatures, long value)
        + setNetworkMultiplier(long multiplier)
        + setServiceBaseFee(String method, long value)
        + setServiceExtraIncludedCount(String method, String signatures, long value)
        + setServiceExtraIncludedCount(String method, Extras extra, long value)
    }

```