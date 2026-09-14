package com.example.domain;

import java.util.Arrays;
import java.util.List;

public enum PunteroEnum {
    AUGUSTO_GONZALEZ("AUGUSTO GONZALEZ", Arrays.asList(
        "4290122", "3318203", "5189597", "5373319", "5940725", "5108226", 
        "4604684", "6255092", "4362697", "5852773", "4507045", "6153796", 
        "6829118", "3534749", "6809024", "7016505", "4485035"
    )),
    ARNALDO_GARCETE("ARNALDO GARCETE", Arrays.asList("4637011", "4275184", "3579041", "3977243", "5569714")),
    FELIPE_RECALDE("FELIPE RECALDE", Arrays.asList("5026481", "4749184", "4155393", "5670142", "6279753", "6131787")),
    ISOLIANO_CACERES("ISOLIANO CACERES", Arrays.asList("3739535", "4013713", "4733433", "3469674", "4655947", "4887083", "4879450", "5873876", "3477030")),
    WILSON_VARELA("WILSON VARELA", Arrays.asList("5010105", "5900636", "5024071", "3425886", "5900639")),
    OLDA_FERNANDEZ("OLDA FERNANDEZ", Arrays.asList("5080926", "3707205", "6900998"));

    private final String displayName;
    private final List<String> cedulas;

    PunteroEnum(String displayName, List<String> cedulas) {
        this.displayName = displayName;
        this.cedulas = cedulas;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getCedulas() {
        return cedulas;
    }

    public static String normalizeCedula(String cedula) {
        if (cedula == null) return "";
        return cedula.trim().replaceFirst("^0+", "");
    }
}
