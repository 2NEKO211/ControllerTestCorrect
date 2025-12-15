package com.example;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

public class X56Reader {

    public static void main(String[] args) throws Exception {
        System.out.println(
                "Environment = " + ControllerEnvironment.getDefaultEnvironment().getClass().getName()
        );

        // 1. 列出所有控制器
        Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();

        System.out.println("检测到的控制器数量 = " + controllers.length);
        for (int i = 0; i < controllers.length; i++) {
            Controller c = controllers[i];
            System.out.println(i + ": " + c.getName() + " / 类型 = " + c.getType());
        }

        if (controllers.length == 0) {
            System.out.println("一个控制器都没检测到。");
            return;
        }

        // ========= 选 Stick（右手摇杆） =========
        Controller stick = null;
        int stickIndex = -1;

        for (int i = 0; i < controllers.length; i++) {
            Controller c = controllers[i];
            String name = c.getName().toLowerCase();

            if (name.contains("x-56") && name.contains("stick")
                    && c.getType() == Controller.Type.STICK) {
                stick = c;
                stickIndex = i;
                System.out.println("\n匹配到 X56 Stick 设备: index = " + i +
                        ", name = " + c.getName() + " / 类型 = " + c.getType());
                break;
            }
        }

        // ========= 选 Throttle（左手油门模块） =========
        Controller throttle = null;
        int throttleIndex = -1;

        for (int i = 0; i < controllers.length; i++) {
            Controller c = controllers[i];
            String name = c.getName().toLowerCase();

            if (name.contains("x-56") && name.contains("throttle")) {
                Component[] comps = c.getComponents();
                System.out.println("\n候选 Throttle index = " + i
                        + "，组件数量 = " + comps.length);

                if (comps.length == 0) {
                    System.out.println("  -> 这个 Throttle 接口没有组件，跳过。");
                    continue;
                }

                throttle = c;
                throttleIndex = i;
                System.out.println("  -> 选择这个作为 Throttle 设备: "
                        + c.getName() + " / 类型 = " + c.getType());
                break;
            }
        }

        if (stick == null && throttle == null) {
            System.out.println("\n既没找到 Stick，也没找到 Throttle，程序退出。");
            return;
        }

        // ========= 拉出两个设备的组件 =========
        Component[] stickComps = null;
        Component[] throttleComps = null;

        if (stick != null) {
            stickComps = stick.getComponents();
            System.out.println("\n=== Stick 组件列表（index = " + stickIndex + "）===");
            System.out.println("Stick 组件数量 = " + stickComps.length);
            for (int i = 0; i < stickComps.length; i++) {
                Component comp = stickComps[i];
                System.out.println("  [S" + i + "] id=" + comp.getIdentifier().getName()
                        + ", analog=" + comp.isAnalog());
            }

        } else {
            System.out.println("\n未找到 Stick 设备。");
        }

        if (throttle != null) {
            throttleComps = throttle.getComponents();
            System.out.println("\n=== Throttle 组件列表（index = " + throttleIndex + "）===");
            System.out.println("Throttle 组件数量 = " + throttleComps.length);
            for (int i = 0; i < throttleComps.length; i++) {
                Component comp = throttleComps[i];
                System.out.println("  [T" + i + "] name=" + comp.getName()
                        + ", id=" + comp.getIdentifier().getName()
                        + ", analog=" + comp.isAnalog());
            }
        } else {
            System.out.println("\n未找到 Throttle 设备。");
        }

        System.out.println("\n开始读取 Stick + Throttle 的按键和轴变化（有变化就输出），按 Ctrl+C 停止。\n");

        float[] lastStick = stickComps != null ? new float[stickComps.length] : null;
        float[] lastThrottle = throttleComps != null ? new float[throttleComps.length] : null;

        // ========= 主循环：同时轮询 Stick + Throttle =========
        while (true) {

            // ----- 轮询 Stick -----
            if (stick != null && stickComps != null) {
                if (!stick.poll()) {
                    System.out.println("Stick 设备丢失连接。");
                } else {
                    for (int i = 0; i < stickComps.length; i++) {
                        Component comp = stickComps[i];
                        float value = comp.getPollData();
                        float last = lastStick[i];

                        if (Math.abs(value - last) < 0.01f) {
                            continue;
                        }
                        lastStick[i] = value;

                        printEvent("Stick", i, comp, value);
                    }
                }
            }

            // ----- 轮询 Throttle -----
            if (throttle != null && throttleComps != null) {
                if (!throttle.poll()) {
                    System.out.println("Throttle 设备丢失连接。");
                } else {
                    for (int i = 0; i < throttleComps.length; i++) {
                        Component comp = throttleComps[i];
                        float value = comp.getPollData();
                        float last = lastThrottle[i];

                        if (Math.abs(value - last) < 0.01f) {
                            continue;
                        }
                        lastThrottle[i] = value;

                        printEvent("Throttle", i, comp, value);
                    }
                }
            }

            Thread.sleep(20);
        }
    }

    // 打印某个组件的变化
    private static void printEvent(String device, int index, Component comp, float value) {
        // 不再用 name，直接用 id（x, y, z, rx, ry, rz, slider, pov, button0...）
        String id = comp.getIdentifier().getName();

        if (!comp.isAnalog()) {
            System.out.println(device + " 按钮变化: [" + index + "] id=" + id
                    + ", value=" + value
                    + (value > 0.5f ? "  <-- 按下" : "  <-- 松开"));
        } else if (comp.getIdentifier() == Component.Identifier.Axis.POV) {
            System.out.println(device + " POV(HAT) 变化: [" + index + "] id=" + id
                    + ", value=" + value);
        } else {
            System.out.println(device + " 轴变化: [" + index + "] id=" + id
                    + ", value=" + value);
        }
    }

}
