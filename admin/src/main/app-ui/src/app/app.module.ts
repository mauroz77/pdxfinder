import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { AppComponent } from './app.component';
import { AppRoutingModule, routingComponents } from "./app-routing.module";
import { SideNavBarComponent } from './side-nav-bar/side-nav-bar.component';
import { TopNavBarComponent } from './top-nav-bar/top-nav-bar.component';
import {MappingService} from "./mapping.service";
import { HttpClientModule} from "@angular/common/http";
import { DatasourceSpecificComponent } from './datasource-specific/datasource-specific.component';
import {FormsModule} from "@angular/forms";
import {DatasourceSpecificSuggestionsComponent} from "./datasource-specific-suggestions/datasource-specific-suggestions.component";
import {CurationManageComponent} from "./curation-manage/curation-manage.component";
import { CurationArchiveComponent } from './curation-archive/curation-archive.component';
import { CurationOrphanComponent } from './curation-orphan/curation-orphan.component';

@NgModule({
  declarations: [
    AppComponent,
    SideNavBarComponent,
    TopNavBarComponent,
      routingComponents,
      DatasourceSpecificComponent,
      DatasourceSpecificSuggestionsComponent,
      CurationManageComponent,
      CurationArchiveComponent,
      CurationOrphanComponent
  ],
  imports: [
    BrowserModule,
      HttpClientModule,
      FormsModule,
      AppRoutingModule
  ],
  providers: [MappingService],
  bootstrap: [AppComponent]
})
export class AppModule { }


