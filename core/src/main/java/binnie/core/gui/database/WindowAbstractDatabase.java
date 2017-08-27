package binnie.core.gui.database;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import binnie.core.api.genetics.IBreedingSystem;
import binnie.core.api.gui.IArea;
import binnie.core.api.gui.events.EventHandlerOrigin;
import binnie.core.gui.geometry.Point;
import net.minecraft.entity.player.EntityPlayer;

import com.mojang.authlib.GameProfile;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import forestry.api.genetics.IAlleleSpecies;
import forestry.api.genetics.IBreedingTracker;
import forestry.api.genetics.IClassification;

import binnie.core.api.gui.IWidget;
import binnie.core.gui.controls.ControlTextEdit;
import binnie.core.gui.controls.listbox.ControlListBox;
import binnie.core.gui.controls.listbox.ControlTextOption;
import binnie.core.gui.controls.page.ControlPage;
import binnie.core.gui.controls.page.ControlPages;
import binnie.core.gui.controls.tab.ControlTab;
import binnie.core.gui.controls.tab.ControlTabBar;
import binnie.core.gui.events.EventTextEdit;
import binnie.core.gui.events.EventValueChanged;
import binnie.core.gui.geometry.CraftGUIUtil;
import binnie.core.api.gui.Alignment;
import binnie.core.gui.minecraft.MinecraftGUI;
import binnie.core.gui.minecraft.Window;
import binnie.core.gui.minecraft.control.ControlHelp;
import binnie.core.gui.window.Panel;
import binnie.core.util.I18N;

public abstract class WindowAbstractDatabase extends Window {
	private final boolean master;
	private int selectionBoxWidth;
	private Map<IDatabaseMode, ModeWidgets> modes;
	private IBreedingSystem system;
	@Nullable
	private Panel panelInformation;
	@Nullable
	private Panel panelSearch;
	@Nullable
	private ControlPages<IDatabaseMode> modePages;
	@Nullable
	private IAlleleSpecies gotoSpecies;

	public WindowAbstractDatabase(final EntityPlayer player, final Side side, final boolean master, final IBreedingSystem system, final int wid) {
		super(100, 192, player, null, side);
		this.selectionBoxWidth = 95;
		this.modes = new HashMap<>();
		this.panelInformation = null;
		this.panelSearch = null;
		this.modePages = null;
		this.gotoSpecies = null;
		this.master = master;
		this.system = system;
		this.selectionBoxWidth = wid;
	}

	public void changeMode(final IDatabaseMode mode) {
		this.modePages.setValue(mode);
	}

	public ControlPages<DatabaseTab> getInfoPages(final IDatabaseMode mode) {
		return this.modes.get(mode).infoPages;
	}

	public boolean isMaster() {
		return this.master;
	}

	public IBreedingSystem getBreedingSystem() {
		return this.system;
	}

	protected ModeWidgets createMode(final IDatabaseMode mode, final ModeWidgets widgets) {
		this.modes.put(mode, widgets);
		return widgets;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void initialiseClient() {
		this.setSize(new Point(176 + this.selectionBoxWidth + 22 + 8, 208));
		this.addEventHandler(EventValueChanged.class, event -> {
			Object value = event.getValue();
			IWidget eventOriginParent = event.getOrigin().getParent();
			if (eventOriginParent instanceof ControlPage && !(value instanceof DatabaseTab)) {
				ControlPage parent = (ControlPage) eventOriginParent;
				if (parent.getValue() instanceof IDatabaseMode) {
					for (IWidget child : parent.getChildren()) {
						if (child instanceof ControlPages) {
							if (value == null) {
								child.hide();
							} else {
								child.show();
								for (IWidget widget : child.getChildren()) {
									if (widget instanceof PageAbstract) {
										PageAbstract pageAbstract = (PageAbstract) widget;
										pageAbstract.onValueChanged(value);
									}
								}
							}
						}
					}
				}
			}
		});
		this.addEventHandler(EventTextEdit.class, EventHandlerOrigin.DIRECT_CHILD, this, event -> {
			for (final ModeWidgets widgets : WindowAbstractDatabase.this.modes.values()) {
				widgets.listBox.setValidator(object -> {
					if (Objects.equals(event.getValue(), "")) {
						return true;
					}
					if (((ControlTextOption) object).getText().toLowerCase().contains(event.getValue().toLowerCase())) {
						return true;
					}
					return false;
				});
			}
		});
		new ControlHelp(this, 4, 4);
		(this.panelInformation = new Panel(this, 24, 24, 144, 176, MinecraftGUI.PanelType.BLACK)).setColor(860416);
		(this.panelSearch = new Panel(this, 176, 24, this.selectionBoxWidth, 160, MinecraftGUI.PanelType.BLACK)).setColor(860416);
		this.modePages = new ControlPages<>(this, 0, 0, this.getSize().xPos(), this.getSize().yPos());
		new ControlTextEdit(this, 176, 184, this.selectionBoxWidth, 16);
		this.createMode(Mode.SPECIES, new ModeWidgets(Mode.SPECIES, this, (area, modePage) -> {
			final GameProfile playerName = this.getUsername();
			final Collection<IAlleleSpecies> speciesList = this.master ? this.system.getAllSpecies() : this.system.getDiscoveredSpecies(this.getWorld(), playerName);
			ControlSpeciesBox controlSpeciesBox = new ControlSpeciesBox(modePage, area.xPos(), area.yPos(), area.width(), area.height());
			controlSpeciesBox.setOptions(speciesList);
			return controlSpeciesBox;
		}));
		this.createMode(Mode.BRANCHES, new ModeWidgets(Mode.BRANCHES, this, (area, modePage) -> {
			final EntityPlayer player = this.getPlayer();
			final GameProfile playerName = player.getGameProfile();
			final Collection<IClassification> speciesList = this.master ? this.system.getAllBranches() : this.system.getDiscoveredBranches(this.getWorld(), playerName);
			ControlBranchBox controlBranchBox = new ControlBranchBox(modePage, area.xPos(), area.yPos(), area.width(), area.height());
			controlBranchBox.setOptions(speciesList);
			return controlBranchBox;
		}));
		this.createMode(Mode.BREEDER, new ModeWidgets(Mode.BREEDER, this, (area, modePage) -> {
			return new ControlListBox(modePage, area.xPos(), area.yPos(), area.width(), area.height(), 12);
		}));
		this.addTabs();
		final ControlTabBar<IDatabaseMode> tab = new ControlTabBar<>(this, 176 + this.selectionBoxWidth, 24, 22, 176, Alignment.RIGHT, this.modePages.getValues(), DatabaseControlTab::new);
		CraftGUIUtil.linkWidgets(tab, this.modePages);
		this.changeMode(Mode.SPECIES);
		for (final IDatabaseMode mode : this.modes.keySet()) {
			this.modes.get(mode).infoTabs = new ControlTabBar<>(this.modes.get(mode).modePage, 8, 24, 16, 176, Alignment.LEFT, this.modes.get(mode).infoPages.getValues());
			CraftGUIUtil.linkWidgets(this.modes.get(mode).infoTabs, this.modes.get(mode).infoPages);
		}
	}

	@Override
	public void initialiseServer() {
		final IBreedingTracker tracker = this.system.getSpeciesRoot().getBreedingTracker(this.getWorld(), this.getUsername());
		if (tracker != null) {
			tracker.synchToPlayer(this.getPlayer());
		}
	}

	@SideOnly(Side.CLIENT)
	protected void addTabs() {
	}

	public void gotoSpecies(final IAlleleSpecies value) {
		if (value != null) {
			this.modePages.setValue(Mode.SPECIES);
			this.changeMode(Mode.SPECIES);
			this.modes.get(this.modePages.getValue()).listBox.setValue(value);
		}
	}

	public void gotoSpeciesDelayed(final IAlleleSpecies species) {
		this.gotoSpecies = species;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void onUpdateClient() {
		super.onUpdateClient();
		if (this.gotoSpecies != null) {
			((WindowAbstractDatabase) this.getTopParent()).gotoSpecies(this.gotoSpecies);
			this.gotoSpecies = null;
		}
	}

	public enum Mode implements IDatabaseMode {
		SPECIES,
		BRANCHES,
		BREEDER;

		@Override
		public String getName() {
			return I18N.localise(DatabaseConstants.MODE_KEY + "." + this.name().toLowerCase());
		}
	}

	public static final class ModeWidgets {
		@FunctionalInterface
		public interface IListBoxCreator {
			ControlListBox createListBox(IArea area, ControlPage<IDatabaseMode> modePage);
		}

		public WindowAbstractDatabase database;
		public ControlPage<IDatabaseMode> modePage;
		public ControlListBox listBox;
		private ControlPages<DatabaseTab> infoPages;
		private ControlTabBar<DatabaseTab> infoTabs;

		public ModeWidgets(IDatabaseMode mode, WindowAbstractDatabase database, IListBoxCreator listBoxCreator) {
			this.database = database;
			this.modePage = new ControlPage<>(database.modePages, 0, 0, database.getSize().xPos(), database.getSize().yPos(), mode);
			final IArea listBoxArea = database.panelSearch.getArea().inset(2);
			this.listBox = listBoxCreator.createListBox(listBoxArea, this.modePage);
			CraftGUIUtil.alignToWidget(this.listBox, database.panelSearch);
			CraftGUIUtil.moveWidget(this.listBox, new Point(2, 2));
			CraftGUIUtil.alignToWidget(this.infoPages = new ControlPages<>(this.modePage, 0, 0, 144, 176), database.panelInformation);
		}
	}

	private static class DatabaseControlTab extends ControlTab<IDatabaseMode> {
		public DatabaseControlTab(int x, int y, int w, int h, IDatabaseMode value) {
			super(x, y, w, h, value);
		}

		@Override
		public String getName() {
			return this.value.getName();
		}
	}
}