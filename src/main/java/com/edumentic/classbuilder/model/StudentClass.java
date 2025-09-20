package com.edumentic.classbuilder.model;

import lombok.Getter;
import lombok.Setter;

/**
 * We have a 'StudentClass' class just in case we need to be sepecific about
 * whch class student go into - we may be provided more contraints around
 * what a teacher can work with etc and may need to have planning clacultions attrirbutes
 * on the individual student classes. at this stage, we just number them
 */
public class StudentClass {

    @Getter @Setter
    private int classNumber;


}
