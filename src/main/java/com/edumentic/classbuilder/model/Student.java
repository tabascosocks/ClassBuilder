package com.edumentic.classbuilder.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class Student {
    private String name;
    private List<Student> mustIncludeFriends;
    private List<Student> shouldIncludeFriends;
    private List<Student> cannotBeWith;
    private List<Student> avoidBeingWith;

    private int numeracy;        // 1-5
    private int literacy;        // 1-5
    private int socialEmotional;
    private Gender gender;

    public String toPrettyString() {
        return "Student {\n" +
                "  name='" + name + "',\n" +
                "  mustIncludeFriends=" + getNames(mustIncludeFriends) + ",\n" +
                "  shouldIncludeFriends=" + getNames(shouldIncludeFriends) + ",\n" +
                "  cannotBeWith=" + getNames(cannotBeWith) + ",\n" +
                "  avoidBeingWith=" + getNames(avoidBeingWith) + ",\n" +
                "  numeracy=" + numeracy + ",\n" +
                "  literacy=" + literacy + ",\n" +
                "  socialEmotional=" + socialEmotional + "\n" +
                "  gender=" + gender + "\n" +
                '}';
    }

    private static String getNames(List<Student> students) {
        if (students == null) return "[]";
        return students.stream()
                .map(s -> s.name)
                .toList()
                .toString();
    }
}
