package com.edumentic.classbuilder.solution;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ClassBuilderGlobalConstraints {
    private int minClassSize;
    private int maxClassSize;

    @Getter
    private static ClassBuilderGlobalConstraints instance;

    static{
        instance = new ClassBuilderGlobalConstraints();
    }


}
