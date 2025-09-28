package com.edumentic.classbuilder.solution;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ClassBuilderConstraints {
    private int minClassSize = 15;
    private int maxClassSize = 30;
    //the scale factor on the total variation between classes
    //on metrics like numeracy, literacy
    private int classMetricVarianceSensitivity = 10;


    private boolean mustIncludeOthers = true;
    private boolean shouldIncludeOthers = true;
    private boolean mustAvoidOthers = true;
    private boolean shouldAvoidOthers = true;
    private boolean balanceNumeracy = true;
    private boolean balanceLiteracy = true;
    private boolean balanceSocialEmotional = true;
    private boolean balanceGender = true;

    @Getter
    private static ClassBuilderConstraints instance;

    static{
        instance = new ClassBuilderConstraints();
    }


}
