/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.imagini.jzookeeperedit;

import org.apache.curator.framework.CuratorFramework;

/**
 *
 * @author dlowe
 */
public class ZKNode {
    private final CuratorFramework zkClient;
    private String label;
    
    public ZKNode(CuratorFramework zkClient, String label) {
        this.zkClient = zkClient;
        this.label = label;
    }
    
    public void setLabel(String label) {
        this.label = label;
    }
    
    public CuratorFramework getClient() {
        return zkClient;
    }
    
    @Override
    public String toString() {
        return label;
    }
}
