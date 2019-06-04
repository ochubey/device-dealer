package io.ochubey.ui;

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;

import com.vaadin.ui.UI;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Label;
import com.vaadin.ui.Button;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalSplitPanel;

import com.vaadin.ui.themes.ValoTheme;
import io.ochubey.devices.Device;
import io.ochubey.devices.repository.DeviceRepository;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by o.chubey on 5/24/18.
 */
@SpringUI
@Theme("valo")
public class DeviceDealerUI extends UI {

    private VerticalLayout layuot;
    private Grid<Device> grid = new Grid<>();

    @Autowired
    private DeviceRepository repository;

    @Override
    protected void init(VaadinRequest request) {
        setupLayout();
        addHeader();
        addGrid();
        addButtons();
    }

    private void addGrid() {
        setSizeFull();
        grid.addColumn(Device::getPlatform).setCaption("Platform").setWidthUndefined();
        grid.addColumn(Device::getDeviceName).setCaption("Device Name");
        grid.addColumn(Device::getPlatformVersion).setCaption("Version");
        grid.addColumn(Device::getServerPort).setCaption("Server Port");
        grid.addColumn(Device::getUdid).setCaption("UDID");
        grid.addColumn(Device::getDeviceStatus).setCaption("Device Status");
        grid.setItems(repository.findAll());
        grid.setWidth("100%");
        grid.setHeight("300px");
        layuot.addComponent(grid);
    }

    private void addButtons() {
        HorizontalLayout mainButtonsLayout = new HorizontalLayout();
        HorizontalLayout secondaryButtonsLayout = new HorizontalLayout();
        VerticalSplitPanel splitPanel = new VerticalSplitPanel();

        Button refresh = new Button("REFRESH");
        refresh.addStyleName(ValoTheme.BUTTON_PRIMARY);
        refresh.addClickListener(e -> grid.setItems(repository.findAll()));
        Button terminateAndroidSessions = new Button("Terminate Android sessions");
        terminateAndroidSessions.addStyleName(ValoTheme.BUTTON_DANGER);
        terminateAndroidSessions.addClickListener(e -> grid.setItems(repository.setAndroidToIdle()));

        Button terminateIphoneSessions = new Button("Terminate iPhone sessions");
        terminateIphoneSessions.addStyleName(ValoTheme.BUTTON_DANGER);
        terminateIphoneSessions.addClickListener(e -> grid.setItems(repository.setIosToIdle()));

        mainButtonsLayout.addComponent(refresh);
        secondaryButtonsLayout.addComponents(terminateAndroidSessions, terminateIphoneSessions);

        layuot.addComponents(mainButtonsLayout, splitPanel, secondaryButtonsLayout);
    }

    private void addHeader() {
        Label header = new Label("Device Dealer");
        header.addStyleName(ValoTheme.LABEL_H1);
        header.setSizeUndefined();
        layuot.addComponent(header);
    }

    private void setupLayout() {
        layuot = new VerticalLayout();
        layuot.setDefaultComponentAlignment(Alignment.MIDDLE_CENTER);
        setContent(layuot);
    }

}
